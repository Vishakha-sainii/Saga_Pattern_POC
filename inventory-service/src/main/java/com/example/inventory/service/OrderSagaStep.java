package com.example.inventory.service;

import org.springframework.stereotype.Component;

import com.example.inventory.saga.OrderClient;


@Component
public class OrderSagaStep implements SagaStep {

    private final OrderClient orderClient;

    public OrderSagaStep(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public void execute(String inventoryId, String sagaId) {
    	orderClient.createOrder(Long.parseLong(inventoryId), sagaId);
    }

    @Override
    public void compensate(String sagaId) {
    	orderClient.updateOrder(Long.parseLong(sagaId));
    }
}
