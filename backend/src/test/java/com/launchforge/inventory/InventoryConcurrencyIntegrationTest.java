package com.launchforge.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.persistence.model.inventory.Inventory;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventoryConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID productId;

    @BeforeEach
    void setUpInventory() {
        productId = resolveProductId();

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 1,
                    reserved_quantity = 0,
                    version = 0
                WHERE product_id = ?
                """,
                productId
        );
    }

    @AfterEach
    void restoreSeedInventory() {
        if (productId == null) {
            return;
        }

        jdbcTemplate.update(
                """
                UPDATE inventory
                SET
                    available_quantity = 8,
                    reserved_quantity = 0
                WHERE product_id = ?
                """,
                productId
        );
    }

    @Test
    void optimisticLockingAllowsOnlyOneSuccessfulConsumption()
            throws Exception {

        Inventory inventory =
                inventoryRepository
                        .findByProduct_Id(
                                productId
                        )
                        .orElseThrow();

        Long initialVersion =
                inventory.getVersion();

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        CyclicBarrier barrier =
                new CyclicBarrier(2);

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );

        AtomicInteger successCount =
                new AtomicInteger();

        List<Throwable> failures =
                java.util.Collections.synchronizedList(
                        new ArrayList<>()
                );

        Callable<Void> consumeTask =
                () -> {
                    try {
                        transactionTemplate.executeWithoutResult(
                                status -> {
                                    Inventory concurrentInventory =
                                            inventoryRepository
                                                    .findByProduct_Id(
                                                            productId
                                                    )
                                                    .orElseThrow();

                                    awaitBarrier(
                                            barrier
                                    );

                                    concurrentInventory.decrease(
                                            1
                                    );

                                    inventoryRepository.saveAndFlush(
                                            concurrentInventory
                                    );
                                }
                        );

                        successCount.incrementAndGet();
                    } catch (Throwable throwable) {
                        failures.add(
                                throwable
                        );
                    }

                    return null;
                };

        List<Future<Void>> futures =
                List.of(
                        executorService.submit(
                                consumeTask
                        ),
                        executorService.submit(
                                consumeTask
                        )
                );

        try {
            for (Future<Void> future : futures) {
                future.get(
                        10,
                        TimeUnit.SECONDS
                );
            }
        } finally {
            executorService.shutdownNow();
        }

        Inventory finalInventory =
                inventoryRepository
                        .findByProduct_Id(
                                productId
                        )
                        .orElseThrow();

        assertThat(
                successCount.get()
        ).isEqualTo(
                1
        );

        assertThat(
                failures
        ).hasSize(
                1
        );

        assertThat(
                exceptionChain(
                        failures.getFirst()
                )
        ).anyMatch(
                throwable ->
                        throwable
                                instanceof ObjectOptimisticLockingFailureException
                                ||
                        throwable
                                instanceof org.hibernate.StaleObjectStateException
        );

        assertThat(
                finalInventory.getAvailableQuantity()
        ).isZero();

        assertThat(
                finalInventory.getVersion()
        ).isEqualTo(
                initialVersion + 1
        );
    }

    private UUID resolveProductId() {
        UUID resolvedProductId =
                jdbcTemplate.queryForObject(
                        """
                        SELECT p.id
                        FROM products p
                        JOIN inventory i
                            ON i.product_id = p.id
                        WHERE p.active = TRUE
                        ORDER BY p.sku
                        LIMIT 1
                        """,
                        UUID.class
                );

        if (resolvedProductId == null) {
            throw new IllegalStateException(
                    "No active product with inventory is available for concurrency tests."
            );
        }

        return resolvedProductId;
    }

    private void awaitBarrier(
            CyclicBarrier barrier
    ) {
        try {
            barrier.await(
                    5,
                    TimeUnit.SECONDS
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Concurrent test barrier failed.",
                    exception
            );
        }
    }

    private List<Throwable> exceptionChain(
            Throwable throwable
    ) {
        List<Throwable> chain =
                new ArrayList<>();

        Throwable current =
                throwable;

        while (current != null) {
            chain.add(
                    current
            );

            current =
                    current.getCause();
        }

        return chain;
    }
}
