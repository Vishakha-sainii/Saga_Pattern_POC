package com.example.saga.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.saga.annotation.SagaOrchestrated;
import com.example.saga.coordinator.DistributedSagaContext;
import com.example.saga.coordinator.SagaCoordinatorService;
import com.example.saga.core.SagaContext;
import com.example.saga.core.SagaManager;

@Aspect
@Component
public class SagaAspect {

    private static final Logger log = LoggerFactory.getLogger(SagaAspect.class);
    
    private final SagaManager sagaManager;

    private static final ThreadLocal<SagaContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<DistributedSagaContext> DISTRIBUTED_CONTEXT = new ThreadLocal<>();

    @Autowired(required = false)
    private SagaCoordinatorService coordinatorService;

    public SagaAspect(SagaManager sagaManager) {
        this.sagaManager = sagaManager;
    }

    @Around("@annotation(sagaOrchestrated)")
    public Object handleSaga(
            ProceedingJoinPoint pjp,
            SagaOrchestrated sagaOrchestrated
    ) throws Throwable {

        // Check if we're in a distributed SAGA context
        DistributedSagaContext distributedContext = DISTRIBUTED_CONTEXT.get();
        
        if (distributedContext != null) {
            // We're already in a distributed SAGA, use that context
            log.debug("Executing method {} in existing distributed SAGA: {}", 
                     pjp.getSignature().getName(), distributedContext.getSagaId());
            return sagaManager.executeWithCoordinator(pjp, distributedContext);
        }
        
        // Traditional SAGA handling for backward compatibility
        SagaContext context = new SagaContext();
        CONTEXT.set(context);

        try {
            return sagaManager.execute(pjp, context);
        } catch (Exception ex) {
            sagaManager.rollback(context);
            throw ex;
        } finally {
            CONTEXT.remove();
        }
    }

    // Optional helper for traditional context
    public static SagaContext context() {
        return CONTEXT.get();
    }
    
    // Helper for distributed context
    public static DistributedSagaContext distributedContext() {
        return DISTRIBUTED_CONTEXT.get();
    }
    
    /**
     * Set distributed context for the current thread.
     * This allows services to participate in a distributed SAGA.
     */
    public static void setDistributedContext(DistributedSagaContext context) {
        DISTRIBUTED_CONTEXT.set(context);
        log.debug("Set distributed SAGA context: {}", context.getSagaId());
    }
    
    /**
     * Clear distributed context for the current thread.
     */
    public static void clearDistributedContext() {
        DistributedSagaContext context = DISTRIBUTED_CONTEXT.get();
        if (context != null) {
            log.debug("Clearing distributed SAGA context: {}", context.getSagaId());
            DISTRIBUTED_CONTEXT.remove();
        }
    }
    
    /**
     * Check if we're currently in a distributed SAGA.
     */
    public static boolean hasDistributedContext() {
        return DISTRIBUTED_CONTEXT.get() != null;
    }
}
