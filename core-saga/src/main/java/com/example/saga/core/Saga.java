package com.example.saga.core;

import com.example.saga.aop.SagaAspect;
import com.example.saga.remote.RemoteSagaStep;

public final class Saga {

    private Saga() {}

    public static void compensate(SagaStep step) {
        SagaAspect.context().addStep(step);
    }
    
    public static void remoteCompensate(
            String service,
            String method,
            String url
    ) {

        SagaAspect.context().addStep(
            new RemoteSagaStep(
                service,
                method,
                url
            )
        );
    }
}
