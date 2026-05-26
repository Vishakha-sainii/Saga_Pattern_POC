package com.example.saga.core;


@FunctionalInterface
public interface SagaStep {
//    void execute(SagaContext context);
//    void rollback(SagaContext context);
    void compensate();

}
