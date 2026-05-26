package com.example.billing.controller;

import com.example.billing.service.SagaRollbackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saga")
public class SagaRollbackController {

    @Autowired
    SagaRollbackService sagaRollbackService;

    @PostMapping("/rollback")
    public void rollback(@RequestParam String sagaId)  {
        try {
			sagaRollbackService.rollback(sagaId);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    }
}
