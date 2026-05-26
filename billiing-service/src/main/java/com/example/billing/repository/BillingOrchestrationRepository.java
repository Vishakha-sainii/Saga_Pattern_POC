package com.example.billing.repository;

import com.example.billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface BillingOrchestrationRepository extends JpaRepository<Bill, Long> {
    Bill findByInventoryId(Long inventoryId);
//    Optional<Object> findByItemId(Long itemId);

//    Bill findByInventoryId(Long inventoryId);
}
