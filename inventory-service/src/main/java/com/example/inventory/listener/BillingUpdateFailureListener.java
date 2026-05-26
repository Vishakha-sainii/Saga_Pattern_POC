package com.example.inventory.listener;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemChreograpyRepository;
//import com.example.saga.events.BillingUpdateFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingUpdateFailureListener {

    private final ItemChreograpyRepository repo;

    public BillingUpdateFailureListener(ItemChreograpyRepository repo) {
        this.repo = repo;
    }

//    @KafkaListener(
//            topics = "billing-update-failed",
//            groupId = "inventory-group"
//    )
//    public void rollbackInventory(BillingUpdateFailedEvent event) {
//
//        Item item = repo.findById(event.itemId()).orElseThrow();
//        item.setName(event.oldName());
//
//        repo.save(item);
//
//        log.warn("Inventory rollback completed for item {}", event.itemId());
//    }
}