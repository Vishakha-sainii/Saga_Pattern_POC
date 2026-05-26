package com.example.inventory.controller;

import com.example.inventory.service.InventoryOrchestrationService;
import com.example.inventory.service.EnhancedInventoryOrchestrationService;
import com.example.inventory.service.OrderSagaOrchestrator;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@Slf4j
@AllArgsConstructor
public class InventoryOrchestrationController {

    
    private final InventoryOrchestrationService inventoryOrchestrationService;
    
    @Autowired
    private EnhancedInventoryOrchestrationService enhancedInventoryOrchestrationService;

// Flow 1 : Inventory service calls order service and then order service calls billing service

    @PostMapping("/create")
    public ResponseEntity<String> createInventory(){
        inventoryOrchestrationService.createInventory("Shoes", 3);
        return ResponseEntity.ok("Item Created successfully");

    }

    @PutMapping("/update")
    public ResponseEntity<String> updateInventory(
            @RequestParam Long ItemId,
            @RequestParam int qty) throws JsonProcessingException {

        inventoryOrchestrationService.updateInventory(ItemId, qty);
        return ResponseEntity.ok("Inventory updated");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@RequestParam Long ItemId) throws JsonProcessingException {
        inventoryOrchestrationService.deleteInventory(ItemId);
        return ResponseEntity.ok("Deleted Successfully");
    }


 // Flow 2: Inventory service calls order service and then inventory calls billing service

    @PostMapping("/create-flow2")
    public ResponseEntity<String> createInventoryFlow2(){
        inventoryOrchestrationService.createInventoryV2("iPhone 13", 1);
        return ResponseEntity.ok("Item Created successfully");

    }

    @PutMapping("/update-flow2")
    public ResponseEntity<String> updateInventoryFlow2(
            @RequestParam Long ItemId,
            @RequestParam int qty) throws JsonProcessingException {

        inventoryOrchestrationService.updateInventoryV2(ItemId, qty);
        return ResponseEntity.ok("Inventory updated");
    }

    @DeleteMapping("/delete-flow2")
    public ResponseEntity<String> deleteInventoryFlow2(@RequestParam Long ItemId) throws JsonProcessingException {
        inventoryOrchestrationService.deleteInventoryV2(ItemId);
        return ResponseEntity.ok("Inventory Deleted Successfully");
    }

    /**
     * Enhanced update endpoint that demonstrates the coordinator approach.
     * This shows how the centralized coordinator solves the compensation issue.
     */
    @PutMapping("/update-enhanced")
    public ResponseEntity<String> updateInventoryEnhanced(
            @RequestParam Long ItemId,
            @RequestParam int qty) throws JsonProcessingException {
        
        log.info(" Starting enhanced update with coordinator approach for item: {}", ItemId);
        
        try {
            enhancedInventoryOrchestrationService.updateInventoryV2Enhanced(ItemId, qty);
            return ResponseEntity.ok(" Enhanced inventory update completed successfully");
        } catch (Exception ex) {
            log.error(" Enhanced inventory update failed: {}", ex.getMessage());
            return ResponseEntity.internalServerError()
                .body(" Enhanced update failed: " + ex.getMessage());
        }
    }

}
