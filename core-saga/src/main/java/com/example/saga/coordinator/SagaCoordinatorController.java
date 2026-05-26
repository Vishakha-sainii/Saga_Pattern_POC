package com.example.saga.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for SAGA coordinator management and monitoring.
 */
@RestController
@RequestMapping("/saga/coordinator")
public class SagaCoordinatorController {
    
    private static final Logger log = LoggerFactory.getLogger(SagaCoordinatorController.class);
    
    @Autowired
    private SagaCoordinatorService coordinatorService;
    
    @Autowired
    private ServiceStepRegistry stepRegistry;
    
    /**
     * Get summary of all active SAGAs.
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, ServiceStepRegistry.SagaSummary>> getActiveSagas() {
        Map<String, ServiceStepRegistry.SagaSummary> summaries = coordinatorService.getActiveSagaSummaries();
        return ResponseEntity.ok(summaries);
    }
    
    /**
     * Get status of a specific SAGA.
     */
    @GetMapping("/status/{sagaId}")
    public ResponseEntity<SagaStatusResponse> getSagaStatus(@PathVariable String sagaId) {
        DistributedSagaContext.SagaStatus status = coordinatorService.getSagaStatus(sagaId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        ServiceStepRegistry.SagaSummary summary = stepRegistry.getSagaSummaries().get(sagaId);
        
        return ResponseEntity.ok(new SagaStatusResponse(
            sagaId, 
            status,
            summary != null ? summary.getStepCount() : 0,
            summary != null ? summary.getServices() : null
        ));
    }
    
    /**
     * Manually trigger rollback for a SAGA.
     */
    @PostMapping("/rollback/{sagaId}")
    public ResponseEntity<String> rollbackSaga(@PathVariable String sagaId) {
        if (!coordinatorService.isSagaActive(sagaId)) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            coordinatorService.rollbackSaga(sagaId);
            log.info("Manual rollback triggered for SAGA: {}", sagaId);
            return ResponseEntity.ok("Rollback initiated for SAGA: " + sagaId);
        } catch (Exception ex) {
            log.error("Failed to rollback SAGA {}: {}", sagaId, ex.getMessage());
            return ResponseEntity.internalServerError()
                .body("Failed to rollback SAGA: " + ex.getMessage());
        }
    }
    
    /**
     * Health check endpoint for the coordinator.
     */
    @GetMapping("/health")
    public ResponseEntity<CoordinatorHealthResponse> health() {
        Map<String, ServiceStepRegistry.SagaSummary> activeSagas = coordinatorService.getActiveSagaSummaries();
        
        return ResponseEntity.ok(new CoordinatorHealthResponse(
            "UP",
            activeSagas.size(),
            System.currentTimeMillis()
        ));
    }
    
    /**
     * Response class for SAGA status.
     */
    public static class SagaStatusResponse {
        private final String sagaId;
        private final DistributedSagaContext.SagaStatus status;
        private final int stepCount;
        private final java.util.List<String> services;
        
        public SagaStatusResponse(String sagaId, DistributedSagaContext.SagaStatus status, 
                                int stepCount, java.util.List<String> services) {
            this.sagaId = sagaId;
            this.status = status;
            this.stepCount = stepCount;
            this.services = services;
        }
        
        public String getSagaId() { return sagaId; }
        public DistributedSagaContext.SagaStatus getStatus() { return status; }
        public int getStepCount() { return stepCount; }
        public java.util.List<String> getServices() { return services; }
    }
    
    /**
     * Response class for coordinator health.
     */
    public static class CoordinatorHealthResponse {
        private final String status;
        private final int activeSagaCount;
        private final long timestamp;
        
        public CoordinatorHealthResponse(String status, int activeSagaCount, long timestamp) {
            this.status = status;
            this.activeSagaCount = activeSagaCount;
            this.timestamp = timestamp;
        }
        
        public String getStatus() { return status; }
        public int getActiveSagaCount() { return activeSagaCount; }
        public long getTimestamp() { return timestamp; }
    }
}