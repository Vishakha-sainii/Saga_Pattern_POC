package com.example.saga.core;

import com.example.saga.coordinator.DistributedSagaContext;
import com.example.saga.coordinator.SagaCoordinatorService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SagaManager {
    
    private static final Logger log = LoggerFactory.getLogger(SagaManager.class);
    
    @Autowired(required = false)
    private SagaCoordinatorService coordinatorService;
    
    public Object execute(ProceedingJoinPoint pjp, SagaContext context) throws Throwable {
        // Start saga execution
        return pjp.proceed();
    }
    
    /**
     * Enhanced execute method that works with distributed coordinator.
     */
    public Object executeWithCoordinator(ProceedingJoinPoint pjp, DistributedSagaContext distributedContext) throws Throwable {
        log.debug("Executing SAGA method with coordinator: {}", distributedContext.getSagaId());
        
        try {
            Object result = pjp.proceed();
            log.debug("SAGA method execution successful for: {}", distributedContext.getSagaId());
            return result;
            
        } catch (Exception ex) {
            log.error("SAGA method execution failed for {}: {}", distributedContext.getSagaId(), ex.getMessage());
            
            // Trigger coordinator-managed rollback
            if (coordinatorService != null) {
                coordinatorService.rollbackSaga(distributedContext.getSagaId());
            } else {
                // Fallback to local rollback if coordinator is not available
                rollbackLocal(distributedContext);
            }
            
            throw ex;
        }
    }

    public void addCompensation(SagaContext context, SagaStep step) {
        context.addStep(step);
    }

    public void rollback(SagaContext context) {
        while (!context.getSteps().isEmpty()) {
            try {
                context.getSteps().pop().compensate();
            } catch (Exception ex) {
                // log and continue rollback
                log.error("Saga rollback failed: {}", ex.getMessage());
            }
        }
    }
    
    /**
     * Rollback using distributed context (fallback when coordinator is not available).
     */
    public void rollbackLocal(DistributedSagaContext distributedContext) {
        log.warn("Performing local rollback for SAGA: {}", distributedContext.getSagaId());
        
        var steps = distributedContext.getCompensationSteps();
        
        for (var step : steps) {
            try {
                if (step.isLocal()) {
                    step.compensate();
                    log.debug("Local compensation successful for service: {}", step.getServiceName());
                }
            } catch (Exception ex) {
                log.error("Local compensation failed for service {} in SAGA {}: {}", 
                         step.getServiceName(), distributedContext.getSagaId(), ex.getMessage());
            }
        }
        
        distributedContext.setStatus(DistributedSagaContext.SagaStatus.COMPENSATED);
    }
}
