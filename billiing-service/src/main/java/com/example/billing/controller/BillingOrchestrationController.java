package com.example.billing.controller;

import com.example.billing.entity.Bill;
import com.example.billing.entity.SagaAction;
import com.example.billing.repository.BillingOrchestrationRepository;
import com.example.billing.repository.SagaActionRepository;
import com.example.billing.service.BillingOrchestrationService;
import com.example.billing.service.SagaLogger;
import com.example.billing.service.SagaRollbackService;
import com.example.saga.annotation.SagaOrchestrated;
import com.example.saga.aop.SagaAspect;
import com.example.saga.core.Saga;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
@Slf4j
public class BillingOrchestrationController {

    @Autowired
    BillingOrchestrationService billingOrchestrationService;


    @PostMapping("/create")
    public void create(@RequestParam String sagaId,
                       @RequestParam Long inventoryId) throws JsonProcessingException {
        billingOrchestrationService.create(sagaId, inventoryId);
    }


    @PutMapping("/update")
    public void update(@RequestParam String sagaId,
                       @RequestParam Long inventoryId) throws JsonProcessingException {
        billingOrchestrationService.update(sagaId, inventoryId);
    }


    @DeleteMapping("/delete")
    public void delete(@RequestParam String sagaId,
                       @RequestParam Long inventoryId) throws JsonProcessingException {
        billingOrchestrationService.delete(sagaId, inventoryId);
    }
}
