package com.example.saga.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.ArrayDeque;
import java.util.Deque;

public class SagaContext {

    private final String sagaId = UUID.randomUUID().toString();
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    public String getSagaId() {
        return sagaId;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }
    
    
    private final Deque<SagaStep> steps = new ArrayDeque<>();

    public void addStep(SagaStep step) {
        steps.push(step); // LIFO for rollback
    }

    Deque<SagaStep> getSteps() {
        return steps;
    }

}
