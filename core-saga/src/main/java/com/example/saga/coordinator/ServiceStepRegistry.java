package com.example.saga.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that tracks all compensation steps for each SAGA across services.
 * Provides centralized access to compensation information for rollback coordination.
 */
@Component
public class ServiceStepRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ServiceStepRegistry.class);
    
    // Map of sagaId -> List of ServiceCompensationSteps
    private final Map<String, List<ServiceCompensationStep>> sagaSteps = new ConcurrentHashMap<>();
    
    // Map of sagaId -> Set of participating services
    private final Map<String, Set<String>> sagaParticipants = new ConcurrentHashMap<>();
    
    /**
     * Register a compensation step for a specific SAGA.
     */
    public void registerStep(String sagaId, ServiceCompensationStep step) {
        sagaSteps.computeIfAbsent(sagaId, k -> new ArrayList<>()).add(step);
        sagaParticipants.computeIfAbsent(sagaId, k -> new HashSet<>()).add(step.getServiceName());
        
        log.debug("Registered compensation step for SAGA {}: {}", sagaId, step);
    }
    
    /**
     * Get all compensation steps for a SAGA in reverse order (for rollback).
     */
    public List<ServiceCompensationStep> getCompensationSteps(String sagaId) {
        List<ServiceCompensationStep> steps = sagaSteps.getOrDefault(sagaId, new ArrayList<>());
        
        // Sort by priority (higher first) then by timestamp (later first)
        steps.sort((a, b) -> {
            int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Long.compare(b.getTimestamp(), a.getTimestamp());
        });
        
        return new ArrayList<>(steps);
    }
    
    /**
     * Get all services participating in a specific SAGA.
     */
    public Set<String> getParticipatingServices(String sagaId) {
        return new HashSet<>(sagaParticipants.getOrDefault(sagaId, Collections.emptySet()));
    }
    
    /**
     * Get steps for a specific service in a SAGA.
     */
    public List<ServiceCompensationStep> getStepsForService(String sagaId, String serviceName) {
        return getCompensationSteps(sagaId).stream()
                .filter(step -> serviceName.equals(step.getServiceName()))
                .toList();
    }
    
    /**
     * Check if a SAGA has any registered steps.
     */
    public boolean hasSteps(String sagaId) {
        return sagaSteps.containsKey(sagaId) && !sagaSteps.get(sagaId).isEmpty();
    }
    
    /**
     * Get the number of steps registered for a SAGA.
     */
    public int getStepCount(String sagaId) {
        return sagaSteps.getOrDefault(sagaId, Collections.emptyList()).size();
    }
    
    /**
     * Clear all steps for a SAGA (called after successful completion).
     */
    public void clearSagaSteps(String sagaId) {
        List<ServiceCompensationStep> removed = sagaSteps.remove(sagaId);
        sagaParticipants.remove(sagaId);
        
        if (removed != null && !removed.isEmpty()) {
            log.debug("Cleared {} compensation steps for completed SAGA {}", removed.size(), sagaId);
        }
    }
    
    /**
     * Get all active SAGA IDs.
     */
    public Set<String> getActiveSagaIds() {
        return new HashSet<>(sagaSteps.keySet());
    }
    
    /**
     * Get summary information about all active SAGAs.
     */
    public Map<String, SagaSummary> getSagaSummaries() {
        Map<String, SagaSummary> summaries = new HashMap<>();
        
        for (String sagaId : sagaSteps.keySet()) {
            List<ServiceCompensationStep> steps = sagaSteps.get(sagaId);
            Set<String> services = sagaParticipants.get(sagaId);
            
            summaries.put(sagaId, new SagaSummary(
                sagaId, 
                steps.size(), 
                services != null ? services.size() : 0,
                services != null ? new ArrayList<>(services) : new ArrayList<>()
            ));
        }
        
        return summaries;
    }
    
    /**
     * Summary information about a SAGA.
     */
    public static class SagaSummary {
        private final String sagaId;
        private final int stepCount;
        private final int serviceCount;
        private final List<String> services;
        
        public SagaSummary(String sagaId, int stepCount, int serviceCount, List<String> services) {
            this.sagaId = sagaId;
            this.stepCount = stepCount;
            this.serviceCount = serviceCount;
            this.services = services;
        }
        
        public String getSagaId() { return sagaId; }
        public int getStepCount() { return stepCount; }
        public int getServiceCount() { return serviceCount; }
        public List<String> getServices() { return services; }
    }
}