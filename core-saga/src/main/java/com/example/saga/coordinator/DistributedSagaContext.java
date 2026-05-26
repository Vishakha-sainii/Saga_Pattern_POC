package com.example.saga.coordinator;

import com.example.saga.core.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Distributed SAGA Context that can be shared across multiple services
 * and maintains state for centralized coordination.
 */
public class DistributedSagaContext {
    
    private static final Logger log = LoggerFactory.getLogger(DistributedSagaContext.class);
    
    private final String sagaId;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final Deque<ServiceCompensationStep> compensationSteps = new ConcurrentLinkedDeque<>();
    private final Set<String> participatingServices = ConcurrentHashMap.newKeySet();
    private volatile SagaStatus status = SagaStatus.ACTIVE;
    private volatile String coordinatorServiceUrl;
    
    public DistributedSagaContext(String sagaId) {
        this.sagaId = sagaId;
        log.debug("Created distributed SAGA context with ID: {}", sagaId);
    }
    
    public String getSagaId() {
        return sagaId;
    }
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public SagaStatus getStatus() {
        return status;
    }
    
    public void setStatus(SagaStatus status) {
        this.status = status;
        log.debug("SAGA {} status changed to: {}", sagaId, status);
    }
    
    public String getCoordinatorServiceUrl() {
        return coordinatorServiceUrl;
    }
    
    public void setCoordinatorServiceUrl(String coordinatorServiceUrl) {
        this.coordinatorServiceUrl = coordinatorServiceUrl;
    }
    
    /**
     * Register a service as participant in this SAGA.
     */
    public void addParticipatingService(String serviceName) {
        participatingServices.add(serviceName);
        log.debug("Service {} joined SAGA {}", serviceName, sagaId);
    }
    
    public Set<String> getParticipatingServices() {
        return Collections.unmodifiableSet(participatingServices);
    }
    
    /**
     * Add a compensation step that can be executed remotely.
     */
    public void addCompensationStep(ServiceCompensationStep step) {
        compensationSteps.addFirst(step); // LIFO for rollback
        log.debug("Added compensation step for service {} in SAGA {}", step.getServiceName(), sagaId);
    }
    
    /**
     * Get all compensation steps in reverse order for rollback.
     */
    public List<ServiceCompensationStep> getCompensationSteps() {
        return new ArrayList<>(compensationSteps);
    }
    
    /**
     * Add a traditional local saga step for backward compatibility.
     */
    public void addStep(SagaStep step) {
        // Wrap traditional saga step in service compensation step
        ServiceCompensationStep serviceStep = new ServiceCompensationStep(
            "LOCAL", 
            step, 
            null, 
            null, 
            0
        );
        addCompensationStep(serviceStep);
    }
    
    /**
     * Clear all steps (used after successful completion).
     */
    public void clearSteps() {
        compensationSteps.clear();
        log.debug("Cleared all compensation steps for SAGA {}", sagaId);
    }
    
    public enum SagaStatus {
        ACTIVE,
        COMPENSATING,
        COMPENSATED,
        COMPLETED,
        FAILED
    }
}