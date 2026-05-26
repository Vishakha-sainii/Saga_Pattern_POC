package com.example.saga.remote;

import com.example.saga.aop.SagaAspect;

public class Saga {

    public static void compensate(Runnable r) {
        SagaAspect.context().addStep(() -> r.run());
    }

    public static void remoteCompensate(
            String service,
            String method,
            String url
    ) {
        SagaAspect.context().addStep(
                new RemoteSagaStep(service, method, url)
        );
    }
}