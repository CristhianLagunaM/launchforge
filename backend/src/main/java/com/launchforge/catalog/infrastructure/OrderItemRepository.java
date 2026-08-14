package com.launchforge.catalog.infrastructure;

import com.launchforge.persistence.model.orders.OrderItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    boolean existsByProduct_Id(UUID productId);
}
