package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.repository.ItemOrchestrationRepository;
import com.example.saga.annotation.SagaOrchestrated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.saga.coordinator.DistributedSaga;
import com.example.saga.coordinator.DistributedSagaContext;

/**
 * Enhanced inventory orchestration service that demonstrates the new 
 * centralized coordinator approach for SAGA management.
 * 
 * This shows how the coordinator pattern would work when the core-saga
 * JAR is updated with the new coordinator components.
 */
@Service
@AllArgsConstructor
@Slf4j
public class EnhancedInventoryOrchestrationService {

    private final ItemOrchestrationRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    SagaLogger sagaLogger;

    @Autowired
    ObjectMapper objectMapper;


@SagaOrchestrated
public void updateInventoryV2Enhanced(Long id, int newQty) throws JsonProcessingException {
    
    // Start distributed SAGA - this creates centralized coordination
    DistributedSagaContext context = DistributedSaga.startDistributedSaga();
    String sagaId = context.getSagaId();
    
    log.info("Starting centralized SAGA: {}", sagaId);
    
    try {
        // Step 1: Update inventory with LOCAL compensation registration
        updateInventoryWithLocalCompensation(id, newQty, sagaId);
        
        // Step 2: Call order service AND register its compensation
        callOrderServiceWithCompensation(id, sagaId);
        
        // Step 3: Call billing service AND register its compensation  
        callBillingServiceWithCompensation(id, sagaId);

        // Complete SAGA - marks all steps as successful
        DistributedSaga.completeDistributedSaga();
        log.info("✅ Centralized SAGA completed successfully: {}", sagaId);
        
    } catch (Exception ex) {
        // Coordinator automatically handles rollback of ALL registered steps
        log.error("❌ SAGA failed, coordinator will handle automatic rollback: {}", ex.getMessage());
        // No need to call rollback manually - coordinator does it automatically
        throw ex;
    }
}

// Step 1: Local operations with compensation
private void updateInventoryWithLocalCompensation(Long id, int newQty, String sagaId) 
        throws JsonProcessingException {
    
    Item item = repo.findById(id).orElseThrow();
    String beforeStateJson = objectMapper.writeValueAsString(item);
    Item beforeState = objectMapper.readValue(beforeStateJson, Item.class);
    
    // Register LOCAL compensation BEFORE making changes
    DistributedSaga.compensate("INVENTORY", () -> {
        repo.save(beforeState);
        sagaLogger.logRollback(sagaId, "ITEM", id);
        log.info("🔄 Inventory compensation executed for SAGA: {}", sagaId);
    });
    
    // Perform the operation
    item.setQuantity(newQty);
    repo.save(item);
    sagaLogger.logUpdate(sagaId, "ITEM", id, beforeStateJson);
    log.info("✅ Inventory updated for SAGA: {}", sagaId);
}

// Step 2: Order service call with compensation registration
private void callOrderServiceWithCompensation(Long id, String sagaId) {
    
    // Register REMOTE compensation BEFORE calling service
    DistributedSaga.remoteCompensate("ORDER", "POST", 
        "http://localhost:9091/orders/compensate/update?sagaId=" + sagaId + "&inventoryId=" + id,
        2); // Priority 2 - higher than billing, executed earlier in rollback
    
    // Call the service through coordinator
    DistributedSaga.callService("ORDER", 
        "http://localhost:9091/orders/update-flow2?sagaId=" + sagaId + "&inventoryId=" + id,
        null, Void.class, "PUT");
        
    log.info("✅ Order service called and compensation registered for SAGA: {}", sagaId);
}

// Step 3: Billing service call with compensation registration
private void callBillingServiceWithCompensation(Long id, String sagaId) {
    
    // Register REMOTE compensation BEFORE calling service
    DistributedSaga.remoteCompensate("BILLING", "POST", 
        "http://localhost:8082/billing/compensate/update?sagaId=" + sagaId + "&inventoryId=" + id,
        1); // Priority 1 - executed last in rollback (LIFO order)
    
    // Call the service through coordinator
    DistributedSaga.callService("BILLING",
        "http://localhost:8082/billing/update?sagaId=" + sagaId + "&inventoryId=" + id,
        null, Void.class, "PUT");
        
    log.info("Billing service called and compensation registered for SAGA: {}", sagaId);
 }

}