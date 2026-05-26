package com.example.saga.coordinator;

import com.example.saga.core.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility class for registering SAGA steps with the coordinator.
 * Provides convenience methods to reduce boilerplate code in services.
 */
@Component
public class SagaStepRegistrar {
    
    private static final Logger log = LoggerFactory.getLogger(SagaStepRegistrar.class);
    
    @Autowired(required = false)
    private SagaCoordinatorService coordinatorService;
    
    /**
     * Register a local compensation step.
     */
    public void registerLocalCompensation(String sagaId, String serviceName, 
                                        SagaStep compensationStep) {
        registerLocalCompensation(sagaId, serviceName, compensationStep, 0);
    }
    
    /**
     * Register a local compensation step with priority.
     */
    public void registerLocalCompensation(String sagaId, String serviceName, 
                                        SagaStep compensationStep, int priority) {
        if (coordinatorService != null) {
            coordinatorService.registerCompensationStep(
                sagaId, serviceName, compensationStep, null, null, priority
            );
            log.debug("Registered local compensation for service {} in SAGA {}", serviceName, sagaId);
        } else {
            log.warn("No coordinator service available for registering local compensation");
        }
    }
    
    /**
     * Register a remote compensation step.
     */
    public void registerRemoteCompensation(String sagaId, String serviceName, 
                                         String compensationUrl, String httpMethod) {
        registerRemoteCompensation(sagaId, serviceName, compensationUrl, httpMethod, 0);
    }
    
    /**
     * Register a remote compensation step with priority.
     */
    public void registerRemoteCompensation(String sagaId, String serviceName, 
                                         String compensationUrl, String httpMethod, int priority) {
        if (coordinatorService != null) {
            coordinatorService.registerCompensationStep(
                sagaId, serviceName, null, compensationUrl, httpMethod, priority
            );
            log.debug("Registered remote compensation for service {} in SAGA {}: {} {}", 
                     serviceName, sagaId, httpMethod, compensationUrl);
        } else {
            log.warn("No coordinator service available for registering remote compensation");
        }
    }
    
    /**
     * Execute a service call through the coordinator.
     */
    public <T> T callService(String sagaId, String serviceName, String serviceUrl, 
                           Object requestBody, Class<T> responseType, String httpMethod) {
        if (coordinatorService != null) {
            return coordinatorService.executeServiceOperation(
                sagaId, serviceName, serviceUrl, requestBody, responseType, httpMethod
            );
        } else {
            throw new IllegalStateException("No coordinator service available for service calls");
        }
    }
    
    /**
     * Convenience method for POST calls.
     */
    public <T> T postToService(String sagaId, String serviceName, String serviceUrl, 
                             Object requestBody, Class<T> responseType) {
        return callService(sagaId, serviceName, serviceUrl, requestBody, responseType, "POST");
    }
    
    /**
     * Convenience method for PUT calls.
     */
    public void putToService(String sagaId, String serviceName, String serviceUrl, Object requestBody) {
        callService(sagaId, serviceName, serviceUrl, requestBody, Void.class, "PUT");
    }
    
    /**
     * Convenience method for DELETE calls.
     */
    public void deleteFromService(String sagaId, String serviceName, String serviceUrl) {
        callService(sagaId, serviceName, serviceUrl, null, Void.class, "DELETE");
    }
    
    /**
     * Check if coordinator service is available.
     */
    public boolean isCoordinatorAvailable() {
        return coordinatorService != null;
    }
}