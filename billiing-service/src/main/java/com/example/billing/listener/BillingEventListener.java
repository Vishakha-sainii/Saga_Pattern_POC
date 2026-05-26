package com.example.billing.listener;

import com.example.billing.entity.Bill;
import com.example.billing.repository.BillingOrchestrationRepository;
//import com.example.saga.events.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingEventListener {

   @Autowired
    BillingOrchestrationRepository repo;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BillingEventListener(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


//    @KafkaListener(topics = "inventory-created", groupId = "billing-group")
//    public void handleInventoryCreated(InventoryCreatedEvent event) {
//
//        try {
//            Bill billing = Bill.builder()
//                   .itemId(event.getItemId())
//                    .status("CREATED")
//                    .previousStatus("NEWLY_CREATED")
//                    .build();
//            if (event.getItemId() % 2 == 0) {
//                log.info("inventory runs successfully billing fails");
//                throw new RuntimeException("Billing creation failed");
//            }
//            repo.save(billing);
//        } catch (Exception ex) {
//
//            // Compensation event
//            kafkaTemplate.send(
//                    "billing-failed",
//                    new BillingFailedEvent(event.getItemId())
//            );
//        }
//    }

//    @KafkaListener(
//            topics = "inventory-updated",
//            groupId = "billing-group"
//    )
//    public void handleInventoryUpdated(InventoryUpdatedEvent event) {
//        Bill bill = (Bill) repo.findByItemId(event.itemId())
//                .orElseThrow();
//
//        String oldStatus = bill.getStatus();
//
//        try {
//            // update billing
//         bill.setStatus("UPDATED");
//            repo.save(bill);
//
//            // simulate failure
//            if (event.itemId() % 2 == 0) {
//                throw new RuntimeException("Billing update failed");
//            }
//
//        } catch (Exception ex) {
//
//            // rollback billing
//            bill.setStatus(oldStatus);
//            repo.save(bill);
//
//            // notify inventory
//            kafkaTemplate.send(
//                    "billing-update-failed",
//                    new BillingUpdateFailedEvent(
//                            event.itemId(),
//                            event.oldName()
//                    )
//            );
//        }
//    }
}

