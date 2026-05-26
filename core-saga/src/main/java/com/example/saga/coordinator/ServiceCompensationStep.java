package com.example.saga.coordinator;

import com.example.saga.core.SagaStep;

/**
 * Represents a compensation step for a specific service in the SAGA.
 * Contains both local and remote execution capabilities.
 */
public class ServiceCompensationStep {
    
    private final String serviceName;
    private final SagaStep localStep;
    private final String compensationUrl;
    private final String httpMethod;
    private final int priority;
    private final long timestamp;
    
    public ServiceCompensationStep(String serviceName, SagaStep localStep, 
                                 String compensationUrl, String httpMethod, int priority) {
        this.serviceName = serviceName;
        this.localStep = localStep;
        this.compensationUrl = compensationUrl;
        this.httpMethod = httpMethod != null ? httpMethod.toUpperCase() : "DELETE";
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public SagaStep getLocalStep() {
        return localStep;
    }
    
    public String getCompensationUrl() {
        return compensationUrl;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Execute the compensation step.
     * If it has a local step, execute that; otherwise use remote compensation.
     */
    public void compensate() throws Exception {
        if (localStep != null) {
            localStep.compensate();
        } else if (compensationUrl != null) {
            // This will be handled by the coordinator service
            throw new UnsupportedOperationException(
                "Remote compensation should be handled by coordinator service"
            );
        }
    }
    
    /**
     * Check if this is a remote compensation step.
     */
    public boolean isRemote() {
        return localStep == null && compensationUrl != null;
    }
    
    /**
     * Check if this is a local compensation step.
     */
    public boolean isLocal() {
        return localStep != null;
    }
    
    @Override
    public String toString() {
        return String.format("ServiceCompensationStep{service='%s', url='%s', method='%s', priority=%d}", 
                           serviceName, compensationUrl, httpMethod, priority);
    }
}