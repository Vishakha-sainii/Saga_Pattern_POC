package com.example.saga.sagaCommon;

public interface SagaStep {
    void execute();
    void rollback();
}
