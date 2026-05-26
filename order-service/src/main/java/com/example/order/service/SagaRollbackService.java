package com.example.order.service;


import com.example.order.entity.Orders;
import com.example.order.entity.SagaAction;
import com.example.order.repository.SagaActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
@Component
public class SagaRollbackService {

    @Autowired
    SagaActionRepository sagaActionRepo;

    @Autowired
    ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void rollback(String sagaId) throws JsonProcessingException {
        List<SagaAction> actions =
                sagaActionRepo.findBySagaIdOrderByIdDesc(sagaId);

        for (SagaAction action : actions) {
            switch (action.getOperation()) {
                case "CREATE" -> em.remove(
                        em.find(
                                resolveEntity(action.getEntityName()),
                                action.getEntityId()
                        )
                );

                case "UPDATE", "DELETE" -> {
                    if (action.getBeforeState() == null || action.getBeforeState().isBlank()) {
                        throw new IllegalStateException(
                                "Missing before_state for rollback. sagaActionId=" + action.getId()
                        );
                    }

                    em.merge(
                            mapper.readValue(
                                    action.getBeforeState(),
                                    resolveEntity(action.getEntityName())
                            )
                    );
                }
            }
        }
    }

    private Class<?> resolveEntity(String entityName) {
        return switch (entityName) {
            case "ORDER" -> Orders.class;
            default ->
                    throw new IllegalArgumentException(
                            "Unknown entity: " + entityName
                    );
        };
    }
}
