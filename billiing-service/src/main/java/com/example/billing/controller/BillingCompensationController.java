package com.example.billing.controller;

import com.example.billing.service.BillingOrchestrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Standardized compensation controller for billing service.
 * Provides consistent rollback endpoints that can be called by the coordinator.
 */
@RestController
@RequestMapping("/billing/compensate")
@Slf4j
public class BillingCompensationController {

    @Autowired
    private BillingOrchestrationService billingOrchestrationService;
    
    /**
     * Compensate for billing creation (delete the created bill).
     */
    @PostMapping("/create")
    public ResponseEntity<String> compensateCreate(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            log.info(" Compensating billing creation for SAGA {} - inventory {}", sagaId, inventoryId);
            billingOrchestrationService.compensateBillingCreate(sagaId, inventoryId);
            log.info("Billing creation compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Billing creation compensated successfully");
            
        } catch (Exception ex) {
            log.error(" Billing creation compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Billing creation compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Compensate for billing update (restore previous state).
     */
    @PostMapping("/update")  
    public ResponseEntity<String> compensateUpdate(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            log.info("Compensating billing update for SAGA {} - inventory {}", sagaId, inventoryId);
            billingOrchestrationService.compensateBillingUpdate(sagaId, inventoryId);
            log.info(" Billing update compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Billing update compensated successfully");
            
        } catch (Exception ex) {
            log.error(" Billing update compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Billing update compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Compensate for billing deletion (restore the deleted bill).
     */
    @PostMapping("/delete")
    public ResponseEntity<String> compensateDelete(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            log.info(" Compensating billing deletion for SAGA {} - inventory {}", sagaId, inventoryId);
            billingOrchestrationService.compensateBillingDelete(sagaId, inventoryId);
            log.info(" Billing deletion compensation successful for SAGA {}", sagaId);
            return ResponseEntity.ok("Billing deletion compensated successfully");
            
        } catch (Exception ex) {
            log.error(" Billing deletion compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Billing deletion compensation failed: " + ex.getMessage());
        }
    }
    
    /**
     * Check if compensation is possible for a given billing record.
     */
    @GetMapping("/can-compensate")
    public ResponseEntity<Boolean> canCompensate(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        try {
            boolean canCompensate = billingOrchestrationService.canCompensateBilling(sagaId, inventoryId);
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
        return ResponseEntity.ok("Billing compensation service is healthy");
    }
    
    /**
     * Simulate billing failure for testing the coordinator rollback.
     * This endpoint intentionally fails after a partial operation to test
     * the coordinator's ability to handle mid-operation failures.
     */
    @PostMapping("/simulate-failure")
    public ResponseEntity<String> simulateFailure(
            @RequestParam String sagaId,
            @RequestParam Long inventoryId) {
        
        log.warn("🧪 Simulating billing failure for SAGA {} - inventory {}", sagaId, inventoryId);
        
        try {
            // Simulate partial operation - create bill but then fail
            billingOrchestrationService.simulatePartialFailure(sagaId, inventoryId);
            // This should never be reached
            return ResponseEntity.ok("This should not happen");
            
        } catch (Exception ex) {
            log.error("💥 Billing simulation failure for SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Simulated billing failure: " + ex.getMessage());
        }
    }
}