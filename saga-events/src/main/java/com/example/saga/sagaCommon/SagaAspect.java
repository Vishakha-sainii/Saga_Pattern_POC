package com.example.saga.sagaCommon;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class SagaAspect {

    @Around("@annotation(com.example.saga.sagaCommon.DistributedSaga)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        try {
            return pjp.proceed();
        } catch (Exception ex) {
            SagaContext.get().rollback();
            throw ex;
        } finally {
            SagaContext.clear();
        }
    }
}
