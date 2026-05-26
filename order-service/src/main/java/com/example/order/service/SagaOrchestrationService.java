package com.example.order.service;

import com.example.order.entity.Orders;
import com.example.order.repository.OrdersRepository;
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
public class SagaOrchestrationService {
    @Autowired
    OrdersRepository orderRepo;
    @Autowired
    SagaLogger sagaLogger;
    @Autowired
    ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @SagaOrchestrated
    public void create(Long inventoryId, String sagaId) {
//                int a = 1/0;
        Orders order = orderRepo.save(
                Orders.builder()
                        .inventoryId(inventoryId)
                        .status("CREATED")
                        .build()
        );
        sagaLogger.logCreate(sagaId, "ORDER", order.getId());
        Long orderId = order.getId();
        Saga.compensate(() -> {
//            log.info("Rolling back order for sagaId: {}, itemId: {}", sagaId, inventoryId);
            orderRepo.deleteById(orderId);
            sagaLogger.logRollback(sagaId, "ORDER", orderId);
//            log.info("Order rollback completed for itemId: {}", inventoryId);
        });

        restTemplate.postForObject(
                "http://localhost:8082/billing/create?sagaId=" + sagaId +
                        "&inventoryId=" + inventoryId,
                null, Void.class
        );
    }

    @SagaOrchestrated
    public void update(String sagaId, Long inventoryId) throws JsonProcessingException {
        Orders order = orderRepo.findByInventoryId(inventoryId);
        log.info("=========order" + order.getId());
        String beforeStateJson = objectMapper.writeValueAsString(order);
        Orders beforeState = objectMapper.readValue(beforeStateJson, Orders.class);


        Saga.compensate(() -> {
            orderRepo.save(beforeState);
            sagaLogger.logRollback(sagaId, "ORDER", order.getId());
        });

        order.setStatus("UPDATED");
        orderRepo.save(order);
//int a = 9/0;
        sagaLogger.logUpdate(
                sagaId,
                "ORDER",
                order.getId(),
                beforeState
        );

        restTemplate.put(
                "http://localhost:8082/billing/update?sagaId=" + sagaId +
                        "&inventoryId=" + inventoryId,
                null,
                Void.class
        );
    }


    @SagaOrchestrated
    public void delete(String sagaId, Long inventoryId) throws JsonProcessingException {
        Orders order = orderRepo.findByInventoryId(inventoryId);

        String beforeStateJson = objectMapper.writeValueAsString(order);
        Orders beforeState = objectMapper.readValue(beforeStateJson, Orders.class);

        Saga.compensate(() -> {
            orderRepo.save(beforeState); // re-insert
            sagaLogger.logRollback(sagaId, "ORDER", order.getId());
        });
        orderRepo.delete(order);
//        int c = 6/0;
        sagaLogger.logDelete(
                sagaId,
                "ORDER",
                order.getId(),
                beforeState
        );
       log.info("Calling billing service for sagaId {}, inventoryId {}", sagaId, inventoryId);
        restTemplate.delete(
                "http://localhost:8082/billing/delete?sagaId=" + sagaId +
                        "&inventoryId=" + inventoryId,
                null,
                Void.class
        );
    }

    @SagaOrchestrated
     public Long createOrderV2(Long inventoryId, String sagaId) {

        Orders order = orderRepo.save(
                Orders.builder()
                        .inventoryId(inventoryId)
                        .status("CREATED")
                        .build()
        );

        sagaLogger.logCreate(
                sagaId,
                "ORDER",
                order.getId()
        );

        return order.getId();
    }

    public void rollbackOrder(Long orderId, String sagaId) {
        orderRepo.deleteById(orderId);
        sagaLogger.logRollback(
                sagaId,
                "ORDER",
                orderId
        );
    }


    @SagaOrchestrated
    public void updateOrderV2(String sagaId, Long inventoryId) throws JsonProcessingException {
        Orders order = orderRepo.findByInventoryId(inventoryId);
        log.info("=========order=======" + order.getId());
        String beforeStateJson = objectMapper.writeValueAsString(order);
        Orders beforeState = objectMapper.readValue(beforeStateJson, Orders.class);
//        sagaLogger.logUpdate(
//                sagaId,
//                "ORDER",
//                order.getId(),
//                beforeState
//        );
//        Saga.compensate(() -> {
//            orderRepo.save(beforeState);
//            sagaLogger.logRollback(sagaId, "ORDER", order.getId());
//        });

        order.setStatus("UPDATED");
        orderRepo.save(order);
// int a = 9/0;
        sagaLogger.logUpdate(
                sagaId,
                "ORDER",
                order.getId(),
                beforeState
        );
    }


    @SagaOrchestrated
    public void deleteOrderV2(String sagaId, Long inventoryId) throws JsonProcessingException {
        Orders order = orderRepo.findByInventoryId(inventoryId);

        String beforeStateJson = objectMapper.writeValueAsString(order);
        Orders beforeState = objectMapper.readValue(beforeStateJson, Orders.class);
//int c = 3/0;
        Saga.compensate(() -> {
            orderRepo.save(beforeState); // re-insert
            sagaLogger.logRollback(sagaId, "ORDER", order.getId());
        });
        orderRepo.delete(order);
//        int c = 6/0;
        sagaLogger.logDelete(
                sagaId,
                "ORDER",
                order.getId(),
                beforeState
        );
    }
    
    // ===============================================
    // STANDARDIZED COMPENSATION METHODS
    // ===============================================
    
    /**
     * Compensate for order update by restoring previous state.
     * This method is called by the coordinator when billing fails after order update.
     */
    public void compensateOrderUpdate(String sagaId, Long inventoryId) throws JsonProcessingException {
        log.info("Starting compensation for order update - SAGA: {}, inventoryId: {}", sagaId, inventoryId);
        
        try {
            // Find the current order
            Orders currentOrder = orderRepo.findByInventoryId(inventoryId);
            if (currentOrder == null) {
                log.warn("No order found for inventoryId: {} in SAGA: {}", inventoryId, sagaId);
                return;
            }
        String beforeStateJson = sagaLogger.getBeforeState(sagaId, "ORDER", currentOrder.getId());
            Orders originalState = objectMapper.readValue(beforeStateJson, Orders.class);
            
            // ✅ Restore to original state
            currentOrder.setStatus(originalState.getStatus()); // "CREATED" instead of "UPDATED"
            orderRepo.save(currentOrder);
            
            sagaLogger.logRollback(sagaId, "ORDER", currentOrder.getId());
            log.info("✅ Order restored to original status: {} for SAGA: {}", 
                    originalState.getStatus(), sagaId);
                    
        } catch (Exception ex) {
            log.error(" Order update compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            throw new RuntimeException("Order update compensation failed", ex);
        }
    }
    
    /**
     * Compensate for order deletion by restoring the deleted order.
     */
    public void compensateOrderDelete(String sagaId, Long inventoryId) throws JsonProcessingException {
        log.info("Starting compensation for order deletion - SAGA: {}, inventoryId: {}", sagaId, inventoryId);
        
        try {
            // For delete compensation, we would need to restore from audit log or backup
            // This is a simplified implementation - in practice, you'd restore from audit trail
            
            Orders restoredOrder = Orders.builder()
                .inventoryId(inventoryId)
                .status("RESTORED")  // Mark as restored to indicate it was compensated
                .build();
                
            orderRepo.save(restoredOrder);
            sagaLogger.logRollback(sagaId, "ORDER", restoredOrder.getId());
            
            log.info("✅ Order deletion compensation completed for SAGA: {}", sagaId);
            
        } catch (Exception ex) {
            log.error("❌ Order deletion compensation failed for SAGA {}: {}", sagaId, ex.getMessage());
            throw new RuntimeException("Order deletion compensation failed", ex);
        }
    }
    
    /**
     * Check if compensation is possible for a given order.
     */
    public boolean canCompensateOrder(String sagaId, Long orderId, Long inventoryId) {
        try {
            if (orderId != null) {
                // Check if order exists by ID
                return orderRepo.existsById(orderId);
            } else if (inventoryId != null) {
                // Check if order exists by inventory ID
                Orders order = orderRepo.findByInventoryId(inventoryId);
                return order != null;
            }
            return false;
        } catch (Exception ex) {
            log.error("Error checking compensation possibility for SAGA {}: {}", sagaId, ex.getMessage());
            return false;
        }
    }
    
    /**
     * Utility method to determine the previous status for compensation.
     * This is a simplified approach - in practice, you'd store the previous state.
     */
    private String determinePreviousStatus(String currentStatus) {
        return switch (currentStatus) {
            case "UPDATED" -> "CREATED";
            case "PROCESSED" -> "UPDATED";
            case "COMPLETED" -> "PROCESSED";
            default -> "CREATED";
        };
    }
}

