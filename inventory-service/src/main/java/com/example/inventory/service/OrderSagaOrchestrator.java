package com.example.inventory.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class OrderSagaOrchestrator {

    private final List<SagaStep> steps;

    public OrderSagaOrchestrator(
            OrderSagaStep inventory,
            OrderSagaStep order
//            BillingSagaStep billing
    ) {
        this.steps = List.of(inventory, order);
    }

    public void executeSaga(String inventoryId, String sagaId) {
        List<SagaStep> completedSteps = new ArrayList<>();

        try {
            for (SagaStep step : steps) {
                step.execute(inventoryId, sagaId);
                completedSteps.add(step);
            }
        } catch (Exception ex) {
            rollback(completedSteps, sagaId);
            throw ex;
        }
    }

    private void rollback(List<SagaStep> completedSteps, String sagaId) {
        Collections.reverse(completedSteps);
        for (SagaStep step : completedSteps) {
            try {
                step.compensate(sagaId);
            } catch (Exception e) {
               
            }
        }
    }
}
