package com.example.saga.coordinator;

import com.example.saga.aop.SagaAspect;
import com.example.saga.core.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Enhanced SAGA utility class that works with the distributed coordinator.
 * Provides both traditional and coordinator-based compensation registration.
 */
@Component
public class DistributedSaga {
    
    private static final Logger log = LoggerFactory.getLogger(DistributedSaga.class);
    
    private static ApplicationContext applicationContext;
    
    @Autowired
    public void setApplicationContext(ApplicationContext context) {
        DistributedSaga.applicationContext = context;
    }
    
    /**
     * Register a local compensation step with the distributed coordinator.
     */
    public static void compensate(String serviceName, SagaStep step) {
        compensate(serviceName, step, 0);
    }
    
    /**
     * Register a local compensation step with priority.
     */
    public static void compensate(String serviceName, SagaStep step, int priority) {
        DistributedSagaContext distributedContext = SagaAspect.distributedContext();
        
        if (distributedContext != null) {
            // Use coordinator if available
            SagaCoordinatorService coordinator = getCoordinatorService();
            if (coordinator != null) {
                coordinator.registerCompensationStep(
                    distributedContext.getSagaId(), 
                    serviceName, 
                    step, 
                    null, 
                    null, 
                    priority
                );
                return;
            }
            
            // Fallback to distributed context
            ServiceCompensationStep serviceStep = new ServiceCompensationStep(
                serviceName, step, null, null, priority
            );
            distributedContext.addCompensationStep(serviceStep);
            
        } else {
            // Fallback to traditional SAGA
            var traditionalContext = SagaAspect.context();
            if (traditionalContext != null) {
                traditionalContext.addStep(step);
            }
        }
        
        log.debug("Registered local compensation for service: {}", serviceName);
    }
    
    /**
     * Register a remote compensation step.
     */
    public static void remoteCompensate(String serviceName, String httpMethod, 
                                      String compensationUrl) {
        remoteCompensate(serviceName, httpMethod, compensationUrl, 0);
    }
    
    /**
     * Register a remote compensation step with priority.
     */
    public static void remoteCompensate(String serviceName, String httpMethod, 
                                      String compensationUrl, int priority) {
        DistributedSagaContext distributedContext = SagaAspect.distributedContext();
        
        if (distributedContext != null) {
            // Use coordinator if available
            SagaCoordinatorService coordinator = getCoordinatorService();
            if (coordinator != null) {
                coordinator.registerCompensationStep(
                    distributedContext.getSagaId(), 
                    serviceName, 
                    null, 
                    compensationUrl, 
                    httpMethod, 
                    priority
                );
                return;
            }
            
            // Fallback to distributed context
            ServiceCompensationStep serviceStep = new ServiceCompensationStep(
                serviceName, null, compensationUrl, httpMethod, priority
            );
            distributedContext.addCompensationStep(serviceStep);
            
        } else {
            // Fallback to traditional remote SAGA step
            com.example.saga.core.Saga.remoteCompensate(serviceName, httpMethod, compensationUrl);
        }
        
        log.debug("Registered remote compensation for service {}: {} {}", 
                 serviceName, httpMethod, compensationUrl);
    }
    
    /**
     * Execute a service call through the coordinator.
     */
    public static <T> T callService(String serviceName, String serviceUrl, 
                                  Object requestBody, Class<T> responseType, 
                                  String httpMethod) {
        
        DistributedSagaContext distributedContext = SagaAspect.distributedContext();
        
        if (distributedContext != null) {
            SagaCoordinatorService coordinator = getCoordinatorService();
            if (coordinator != null) {
                return coordinator.executeServiceOperation(
                    distributedContext.getSagaId(), 
                    serviceName, 
                    serviceUrl, 
                    requestBody, 
                    responseType, 
                    httpMethod
                );
            }
        }
        
        // Fallback to direct RestTemplate call
        log.warn("No coordinator available, falling back to direct service call: {}", serviceUrl);
        throw new UnsupportedOperationException(
            "Direct service calls not supported without coordinator. Use traditional approach."
        );
    }
    
    /**
     * Get the coordinator service from Spring context.
     */
    private static SagaCoordinatorService getCoordinatorService() {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(SagaCoordinatorService.class);
            } catch (Exception ex) {
                log.debug("Coordinator service not available: {}", ex.getMessage());
            }
        }
        return null;
    }
    
    /**
     * Start a new distributed SAGA.
     */
    public static DistributedSagaContext startDistributedSaga() {
        SagaCoordinatorService coordinator = getCoordinatorService();
        if (coordinator != null) {
            DistributedSagaContext context = coordinator.startSaga();
            SagaAspect.setDistributedContext(context);
            return context;
        }
        
        throw new IllegalStateException("Cannot start distributed SAGA without coordinator service");
    }
    
    /**
     * Complete the current distributed SAGA.
     */
    public static void completeDistributedSaga() {
        DistributedSagaContext context = SagaAspect.distributedContext();
        if (context != null) {
            SagaCoordinatorService coordinator = getCoordinatorService();
            if (coordinator != null) {
                coordinator.completeSaga(context.getSagaId());
            }
            SagaAspect.clearDistributedContext();
        }
    }
}