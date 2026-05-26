package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemChreograpyRepository;
//import com.example.saga.events.InventoryCreatedEvent;
//import com.example.saga.events.InventoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class InventoryChoreographyService {

    @Autowired
    ItemChreograpyRepository repo;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryChoreographyService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void createItem(String name, int qty) {
       Item item = repo.save(
               Item.builder()
                       .name(name)
//                       .previousName("Newly_Added")
                       .quantity(qty)
                       .build()
       );
       log.info("Item saved successfully for id {}", item.getId());
//       kafkaTemplate.send(
//               "inventory-created",
//               new InventoryCreatedEvent(item.getId(), item.getName(), item.getQuantity())
//       );
    }

    public void updateItem(Long itemId, String newName) {

        Item item = repo.findById(itemId).orElseThrow();
        String oldName = item.getName();

        // update inventory
        item.setName(newName);
        repo.save(item);

        log.info("Inventory updated, publishing event");

//        kafkaTemplate.send(
//                "inventory-updated",
//                new InventoryUpdatedEvent(item.getId(), oldName)
//        );
    }

//    @EventListener
//    public void onBillingFailed(BillingFailedEvent event) {
//
//        Item item = repo.findById(event.getItemId()).orElseThrow();
//        repo.delete(item);
//    }
}
