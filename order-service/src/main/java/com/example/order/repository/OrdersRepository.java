package com.example.order.repository;

import com.example.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    Orders findByInventoryId(Long inventoryId);

    void deleteByInventoryId(Long inventoryId);
}
