package com.launchforge.orders.infrastructure;

import com.launchforge.persistence.model.orders.CustomerOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<CustomerOrder> findById(UUID id);

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    Optional<CustomerOrder> findByCustomer_IdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "items", "items.product"})
    List<CustomerOrder> findAllByCustomer_IdOrderByCreatedAtDesc(UUID customerId);
}
