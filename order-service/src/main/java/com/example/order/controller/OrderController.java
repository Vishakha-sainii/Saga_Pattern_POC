package com.example.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.order.service.SagaOrchestrationService;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    SagaOrchestrationService sagaOrchestrationService;

  @PostMapping("/create")
    public void create(@RequestParam Long inventoryId,
                       @RequestParam String sagaId) throws JsonProcessingException {
        sagaOrchestrationService.create(inventoryId, sagaId);

    }

    @PutMapping("/update")
    public void updateOrder(@RequestParam String sagaId,
                            @RequestParam Long inventoryId) throws JsonProcessingException {
        sagaOrchestrationService.update(sagaId, inventoryId);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String sagaId,
                       @RequestParam Long inventoryId) throws JsonProcessingException {

        sagaOrchestrationService.delete(sagaId, inventoryId);

    }

    @PostMapping("/create-flow2")
    public Long createOrderV2(@RequestParam Long inventoryId,
                              @RequestParam String sagaId) throws JsonProcessingException{
     return sagaOrchestrationService.createOrderV2(inventoryId, sagaId);
    }

    @PutMapping("/update-flow2")
    public void updateOrderV2(@RequestParam Long inventoryId,@RequestParam String sagaId
                              )throws JsonProcessingException {
        sagaOrchestrationService.updateOrderV2(sagaId, inventoryId);
    }

    @DeleteMapping("/delete-flow2")
    public void deleteOrderV2(@RequestParam String sagaId,
                              @RequestParam Long inventoryId) throws JsonProcessingException {
        sagaOrchestrationService.deleteOrderV2(sagaId, inventoryId);
    }

    @DeleteMapping("/rollback/{orderId}")
    public void rollback(
            @PathVariable String orderId,
            @RequestParam String sagaId
    ) {
        sagaOrchestrationService.rollbackOrder(Long.valueOf(orderId), sagaId);
    }
}
