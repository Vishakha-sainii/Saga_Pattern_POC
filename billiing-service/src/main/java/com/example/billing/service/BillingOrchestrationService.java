package com.example.billing.service;

import com.example.billing.entity.Bill;
import com.example.billing.repository.BillingOrchestrationRepository;
import com.example.saga.annotation.SagaOrchestrated;
import com.example.saga.core.Saga;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class BillingOrchestrationService {

    @Autowired
    BillingOrchestrationRepository repo;
    @Autowired
    SagaLogger sagaLogger;
    @Autowired
    ObjectMapper mapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @SagaOrchestrated
    public void create(String sagaId, Long inventoryId) {
//        int a = 1/0;
        Bill bill = repo.save(
                Bill.builder()
                        .inventoryId(inventoryId)
                        .build()
        );

        // STEP 2 — Log saga action
        sagaLogger.logCreate(sagaId, "BILL", bill.getId());

        Saga.compensate(() -> {
//            log.info("Rolling back inventory for sagaId: {}, itemId: {}", sagaId, bill.getId());
            repo.deleteById(bill.getId());
            sagaLogger.logRollback(sagaId, "BILL", bill.getId());
//            log.info("Billing rollback completed for itemId: {}", inventoryId);
        });
    }

    @SagaOrchestrated
    public void update(String sagaId, Long inventoryId) throws JsonProcessingException {
        Bill bill = repo.findByInventoryId(inventoryId);

        String beforeStateJson = mapper.writeValueAsString(bill);
        Bill beforeState = mapper.readValue(beforeStateJson, Bill.class);
        sagaLogger.logUpdate(
                sagaId,
                "BILL",
                bill.getId(),
                beforeState
        );
//        Saga.compensate(() -> {
//            repo.save(beforeState);
//            sagaLogger.logRollback(sagaId, "BILL", bill.getId());
//        });

        bill.setAmount(100);
        repo.save(bill);
int b = 8/0;
        sagaLogger.logUpdate(
                sagaId,
                "BILL",
                bill.getId(),
                beforeState
        );
    }

    @SagaOrchestrated
    public void delete(String sagaId, Long inventoryId) throws JsonProcessingException {
        Bill bill = repo.findByInventoryId(inventoryId);

        String beforeStateJson = mapper.writeValueAsString(bill.getId());
        Bill beforeState = mapper.readValue(beforeStateJson, Bill.class);

//        String sagaId = SagaAspect.context().getSagaId();

        Saga.compensate(() -> {
            repo.save(beforeState); // re-insert
            sagaLogger.logRollback(sagaId, "BILL", bill.getId());
        });

        repo.delete(bill);

        sagaLogger.logDelete(
                sagaId,
                "BILL",
                bill.getId(),
                beforeState
        );
    }
    
    // ===============================================
    // STANDARDIZED COMPENSATION METHODS
    // ===============================================
    
    /**
     * Compensate for billing creation by deleting the created bill.
     */
    public void compensateBillingCreate(String sagaId, Long inventoryId) {
        log.info("Starting compensation for billing creation - SAGA: {}, inventoryId: {}", sagaId, inventoryId);
        
        try {
            Bill bill = repo.findByInventoryId(inventoryId);
            if (bill != null) {
                repo.deleteById(bill.getId());
                sagaLogger.logRollback(sagaId, "BILL", bill.getId());
                log.info(" Billing creation compensation completed for SAGA: {}", sagaId);
            } else {
                log.warn("No billing record found for inventoryId: {} in SAGA: {}", inventoryId, sagaId);
            }
            
        } catch (Exception ex) {
            log.error(" Billing creation compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            throw new RuntimeException("Billing creation compensation failed", ex);
        }
    }
    
    /**
     * Compensate for billing update by restoring previous state.
     */
    public void compensateBillingUpdate(String sagaId, Long inventoryId) throws JsonProcessingException {
        log.info("Starting compensation for billing update - SAGA: {}, inventoryId: {}", sagaId, inventoryId);
        
        try {
            Bill currentBill = repo.findByInventoryId(inventoryId);
            if (currentBill == null) {
                log.warn("No billing record found for inventoryId: {} in SAGA: {}", inventoryId, sagaId);
                return;
            }

            String beforeStateJson = sagaLogger.getBeforeState(sagaId, "BILL", currentBill.getId());
            Bill originalState = mapper.readValue(beforeStateJson, Bill.class);

            // ✅ Restore to original state
            currentBill.setAmount(originalState.getAmount()); // "CREATED" instead of "UPDATED"
            repo.save(currentBill);

            sagaLogger.logRollback(sagaId, "BILL", currentBill.getId());
            log.info("✅ Billing restored to original amount: {} for SAGA: {}",
                    originalState.getAmount(), sagaId);
            
        } catch (Exception ex) {
            log.error(" Billing update compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            throw new RuntimeException("Billing update compensation failed", ex);
        }
    }
    
    /**
     * Compensate for billing deletion by restoring the deleted bill.
     */
    public void compensateBillingDelete(String sagaId, Long inventoryId) {
        log.info("Starting compensation for billing deletion - SAGA: {}, inventoryId: {}", sagaId, inventoryId);
        
        try {
            // For delete compensation, restore from audit log or recreate
            // This is a simplified implementation - in practice, you'd restore from audit trail
            Bill restoredBill = Bill.builder()
                .inventoryId(inventoryId)
                .amount(0) // Restore with default amount
                .build();
                
            repo.save(restoredBill);
            sagaLogger.logRollback(sagaId, "BILL", restoredBill.getId());
            
            log.info("Billing deletion compensation completed for SAGA: {}", sagaId);
            
        } catch (Exception ex) {
            log.error("Billing deletion compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            throw new RuntimeException("Billing deletion compensation failed", ex);
        }
    }
    
    /**
     * Check if compensation is possible for a given billing record.
     */
    public boolean canCompensateBilling(String sagaId, Long inventoryId) {
        try {
            // Check if billing record exists for the given inventory
            Bill bill = repo.findByInventoryId(inventoryId);
            return bill != null;
        } catch (Exception ex) {
            log.error("Error checking compensation possibility for SAGA {}: {}", sagaId, ex.getMessage());
            return false;
        }
    }
    
    /**
     * Simulate a partial failure in billing for testing the coordinator.
     * This method creates a bill and then throws an exception to test rollback scenarios.
     */
    public void simulatePartialFailure(String sagaId, Long inventoryId) {
        log.warn(" Simulating partial billing failure for SAGA: {} - inventory: {}", sagaId, inventoryId);
        
        // Create the bill (this will succeed)
        Bill bill = repo.save(
            Bill.builder()
                .inventoryId(inventoryId)
                .amount(50) // Partial amount
                .build()
        );
        
        sagaLogger.logCreate(sagaId, "BILL", bill.getId());
        log.info("Bill created for simulation: {}", bill.getId());
        
        // Now simulate failure
        throw new RuntimeException("Simulated billing failure after partial operation - SAGA: " + sagaId);
    }
}


