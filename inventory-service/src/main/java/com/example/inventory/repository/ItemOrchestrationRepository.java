package com.example.inventory.repository;

import com.example.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemOrchestrationRepository extends JpaRepository<Item, Long> {
//    Optional<Object> findByBillingId(Long billingId);
//
//    void deleteByBillingId(Long billingId);
}
