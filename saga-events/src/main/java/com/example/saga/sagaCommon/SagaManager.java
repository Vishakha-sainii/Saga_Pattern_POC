package com.example.saga.sagaCommon;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class SagaManager {

    public void execute() {
        SagaContext.get().execute();
    }

    public void rollback() {
        SagaContext.get().rollback();
    }
}
