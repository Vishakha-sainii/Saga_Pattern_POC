package com.example.saga.coordinator;

import com.example.saga.core.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized SAGA coordinator that manages distributed transactions
 * and rollbacks across multiple services.
 */
@Service
public class SagaCoordinatorService {
    
    private static final Logger log = LoggerFactory.getLogger(SagaCoordinatorService.class);
    
    @Autowired
    private ServiceStepRegistry stepRegistry;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, DistributedSagaContext> activeSagas = new ConcurrentHashMap<>();
    
    /**
     * Start a new distributed SAGA.
     */
    public DistributedSagaContext startSaga() {
        DistributedSagaContext context = new DistributedSagaContext(
            java.util.UUID.randomUUID().toString()
        );
        activeSagas.put(context.getSagaId(), context);
        log.info("Started new SAGA: {}", context.getSagaId());
        return context;
    }
    
    /**
     * Get an existing SAGA context.
     */
    public DistributedSagaContext getSagaContext(String sagaId) {
        return activeSagas.get(sagaId);
    }
    
    /**
     * Register a compensation step with the coordinator.
     */
    public void registerCompensationStep(String sagaId, String serviceName, 
                                       SagaStep localStep, String compensationUrl, 
                                       String httpMethod, int priority) {
        
        ServiceCompensationStep step = new ServiceCompensationStep(
            serviceName, localStep, compensationUrl, httpMethod, priority
        );
        
        stepRegistry.registerStep(sagaId, step);
        
        // Also add to distributed context if it exists
        DistributedSagaContext context = activeSagas.get(sagaId);
        if (context != null) {
            context.addCompensationStep(step);
            context.addParticipatingService(serviceName);
        }
        
        log.debug("Registered compensation step for SAGA {}, service {}", sagaId, serviceName);
    }
    
    /**
     * Execute service operation through the coordinator.
     * This allows the coordinator to track which services are called.
     */
    public <T> T executeServiceOperation(String sagaId, String serviceName, 
                                       String serviceUrl, Object requestBody, 
                                       Class<T> responseType, String httpMethod) {
        
        log.debug("Executing {} operation on service {} for SAGA {}", httpMethod, serviceName, sagaId);
        
        // Add service to participants
        DistributedSagaContext context = activeSagas.get(sagaId);
        if (context != null) {
            context.addParticipatingService(serviceName);
        }
        
        try {
            T result;
            switch (httpMethod.toUpperCase()) {
                case "GET" -> result = restTemplate.getForObject(serviceUrl, responseType);
                case "POST" -> result = restTemplate.postForObject(serviceUrl, requestBody, responseType);
                case "PUT" -> {
                    restTemplate.put(serviceUrl, requestBody);
                    result = null;
                }
                case "DELETE" -> {
                    restTemplate.delete(serviceUrl);
                    result = null;
                }
                default -> throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
            }
            
            log.debug("Successfully executed {} on service {} for SAGA {}", httpMethod, serviceName, sagaId);
            return result;
            
        } catch (Exception ex) {
            log.error("Failed to execute {} on service {} for SAGA {}: {}", 
                     httpMethod, serviceName, sagaId, ex.getMessage());
            
            // Trigger rollback on failure
            rollbackSaga(sagaId);
            throw new RuntimeException("Service operation failed, SAGA rolled back", ex);
        }
    }
    
    /**
     * Complete a SAGA successfully.
     */
    public void completeSaga(String sagaId) {
        DistributedSagaContext context = activeSagas.get(sagaId);
        if (context != null) {
            context.setStatus(DistributedSagaContext.SagaStatus.COMPLETED);
            context.clearSteps();
        }
        
        stepRegistry.clearSagaSteps(sagaId);
        activeSagas.remove(sagaId);
        
        log.info("SAGA {} completed successfully", sagaId);
    }
    
    /**
     * Rollback a SAGA by executing all registered compensation steps.
     */
    public void rollbackSaga(String sagaId) {
        log.warn("Starting rollback for SAGA: {}", sagaId);
        
        DistributedSagaContext context = activeSagas.get(sagaId);
        if (context != null) {
            context.setStatus(DistributedSagaContext.SagaStatus.COMPENSATING);
        }
        
        List<ServiceCompensationStep> steps = stepRegistry.getCompensationSteps(sagaId);
        
        if (steps.isEmpty()) {
            log.warn("No compensation steps found for SAGA: {}", sagaId);
            return;
        }
        
        log.info("Rolling back {} steps for SAGA: {}", steps.size(), sagaId);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (ServiceCompensationStep step : steps) {
            try {
                if (step.isLocal()) {
                    // Execute local compensation
                    step.compensate();
                    log.debug("Local compensation successful for service: {}", step.getServiceName());
                } else if (step.isRemote()) {
                    // Execute remote compensation
                    executeRemoteCompensation(step);
                    log.debug("Remote compensation successful for service: {}", step.getServiceName());
                }
                successCount++;
                
            } catch (Exception ex) {
                failureCount++;
                log.error("Compensation failed for service {} in SAGA {}: {}", 
                         step.getServiceName(), sagaId, ex.getMessage());
                // Continue with other compensations even if one fails
            }
        }
        
        // Update final status
        if (context != null) {
            context.setStatus(failureCount == 0 ? 
                DistributedSagaContext.SagaStatus.COMPENSATED : 
                DistributedSagaContext.SagaStatus.FAILED);
        }
        
        stepRegistry.clearSagaSteps(sagaId);
        activeSagas.remove(sagaId);
        
        log.info("SAGA {} rollback completed: {} successful, {} failed", 
                sagaId, successCount, failureCount);
    }
    
    /**
     * Execute remote compensation via HTTP call.
     */
    private void executeRemoteCompensation(ServiceCompensationStep step) throws Exception {
        String url = step.getCompensationUrl();
        String method = step.getHttpMethod();
        
        log.debug("Executing remote compensation: {} {}", method, url);
        
        switch (method) {
            case "DELETE" -> restTemplate.delete(url);
            case "POST" -> restTemplate.postForObject(url, null, Void.class);
            case "PUT" -> restTemplate.put(url, null);
            case "PATCH" -> restTemplate.patchForObject(url, null, Void.class);
            default -> throw new IllegalArgumentException("Unsupported compensation method: " + method);
        }
    }
    
    /**
     * Get summary of all active SAGAs.
     */
    public Map<String, ServiceStepRegistry.SagaSummary> getActiveSagaSummaries() {
        return stepRegistry.getSagaSummaries();
    }
    
    /**
     * Check if a SAGA is active.
     */
    public boolean isSagaActive(String sagaId) {
        return activeSagas.containsKey(sagaId);
    }
    
    /**
     * Get the status of a SAGA.
     */
    public DistributedSagaContext.SagaStatus getSagaStatus(String sagaId) {
        DistributedSagaContext context = activeSagas.get(sagaId);
        return context != null ? context.getStatus() : null;
    }
}