package com.example.order.controller;

import com.example.order.service.SagaOrchestrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Standardized compensation controller for order service.
 * Provides consistent rollback endpoints that can be called by the coordinator.
 */
@RestController
@RequestMapping("/orders/compensate")
@Slf4j
public class OrderCompensationController {

    @Autowired
    private SagaOrchestrationService sagaOrchestrationService;
    
    /**
     * Compensate for order creation (delete the created order).
     */
    @PostMapping("/create")
    public ResponseEntity<String> compensateCreate(
            @RequestParam String sagaId,
            @RequestParam Long orderId) {
        
        try {
            log.info("Compensating order creation for SAGA {} - deleting order {}", sagaId, orderId);
            sagaOrchestrationService.rollbackOrder(orderId, sagaId);
            log.info("Order creation compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Order creation compensated successfully");
            
        } catch (Exception ex) {
            log.error(" Order creation compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Order creation compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Compensate for order update (restore previous state).
     */
    @PostMapping("/update")  
    public ResponseEntity<String> compensateUpdate(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            log.info("Compensating order update for SAGA {} - inventory {}", sagaId, inventoryId);
            sagaOrchestrationService.compensateOrderUpdate(sagaId, inventoryId);
            log.info("Order update compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Order update compensated successfully");
            
        } catch (Exception ex) {
            log.error(" Order update compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Order update compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Compensate for order deletion (restore the deleted order).
     */
    @PostMapping("/delete")
    public ResponseEntity<String> compensateDelete(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            log.info("Compensating order deletion for SAGA {} - inventory {}", sagaId, inventoryId);
            sagaOrchestrationService.compensateOrderDelete(sagaId, inventoryId);
            log.info(" Order deletion compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Order deletion compensated successfully");
            
        } catch (Exception ex) {
            log.error("Order deletion compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Order deletion compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Check if compensation is possible for a given order.
     */
    @GetMapping("/can-compensate")
    public ResponseEntity<Boolean> canCompensate(
            @RequestParam String sagaId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long inventoryId) {
        
        try {
            boolean canCompensate = sagaOrchestrationService.canCompensateOrder(sagaId, orderId, inventoryId);
            log.debug("Compensation check for SAGA {}: {}", sagaId, canCompensate);
            return ResponseEntity.ok(canCompensate);
            
        } catch (Exception ex) {
            log.error("Failed to check compensation possibility for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.ok(false); // Conservative approach - assume compensation is not possible
        }
    }
    
    /**
     * Health check for compensation endpoints.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order compensation service is healthy");
    }
}