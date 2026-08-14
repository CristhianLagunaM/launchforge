package com.launchforge.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.launchforge.catalog.infrastructure.InventoryRepository;
import com.launchforge.persistence.AbstractPostgresIntegrationTest;
import com.launchforge.persistence.model.inventory.Inventory;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InventoryConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void optimisticLockingAllowsOnlyOneSuccessfulConsumption() throws Exception {
        Inventory inventory = inventoryRepository.findByProduct_Id(PRODUCT_ID).orElseThrow();
        inventory.setAvailableQuantity(1);
        inventory.setReservedQuantity(0);
        inventoryRepository.saveAndFlush(inventory);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = new ArrayList<>();

        Callable<Void> consumeTask = () -> {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Inventory concurrentInventory = inventoryRepository.findByProduct_Id(PRODUCT_ID).orElseThrow();
                    awaitBarrier(barrier);
                    concurrentInventory.decrease(1);
                    inventoryRepository.saveAndFlush(concurrentInventory);
                });
                successCount.incrementAndGet();
            } catch (Throwable throwable) {
                failures.add(throwable);
            }
            return null;
        };

        List<Future<Void>> futures = List.of(
                executorService.submit(consumeTask),
                executorService.submit(consumeTask)
        );

        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdownNow();

        Inventory finalInventory = inventoryRepository.findByProduct_Id(PRODUCT_ID).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failures).hasSize(1);
        assertThat(rootCause(failures.getFirst())).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(finalInventory.getAvailableQuantity()).isZero();
        assertThat(finalInventory.getVersion()).isEqualTo(1L);
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Concurrent test barrier failed.", exception);
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
