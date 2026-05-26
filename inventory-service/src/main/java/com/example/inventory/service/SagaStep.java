package com.example.inventory.service;

public interface SagaStep {

    void execute(String inventoryId, String sagaId);

    void compensate(String sagaId);
}
