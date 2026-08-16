package com.launchforge.orders.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.launchforge.persistence.model.orders.CustomerOrder;
import com.launchforge.persistence.model.orders.OrderStatus;

public interface OrderRepository
        extends JpaRepository<CustomerOrder, UUID> {

    @Override
    @SuppressWarnings("null")
    @EntityGraph(
            attributePaths = {
                    "customer",
                    "items",
                    "items.product"
            }
    )
    Optional<CustomerOrder> findById(UUID id);

    @EntityGraph(
            attributePaths = {
                    "customer",
                    "items",
                    "items.product"
            }
    )
    Optional<CustomerOrder> findByCustomer_IdAndIdempotencyKey(
            UUID customerId,
            String idempotencyKey
    );

    @EntityGraph(
            attributePaths = {
                    "customer",
                    "items",
                    "items.product"
            }
    )
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(
            attributePaths = {
                    "customer",
                    "items",
                    "items.product"
            }
    )
    List<CustomerOrder> findAllByCustomer_IdOrderByCreatedAtDesc(
            UUID customerId
    );

    long countByCustomer_IdAndStatusInAndCreatedAtGreaterThanEqual(
            UUID customerId,
            List<OrderStatus> statuses,
            Instant createdAt
    );
}
