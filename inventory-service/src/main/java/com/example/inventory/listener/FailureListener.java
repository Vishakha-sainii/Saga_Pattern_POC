package com.example.inventory.listener;

import com.example.inventory.repository.ItemChreograpyRepository;
//import com.example.saga.events.BillingFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FailureListener {

    @Autowired
    ItemChreograpyRepository repo;


//    @KafkaListener(topics = "billing-failed", groupId = "inventory-group")
//    public void rollbackInventory(BillingFailedEvent event) {
//        log.info("Roll backing saved data for item id {}", event.getItemId());
//        repo.deleteById(event.getItemId());
//    }
}
