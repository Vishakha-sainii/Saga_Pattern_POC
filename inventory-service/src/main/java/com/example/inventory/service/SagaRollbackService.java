package com.example.inventory.service;

import com.example.inventory.entity.Item;
import com.example.inventory.entity.SagaAction;
import com.example.inventory.repository.SagaActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                case "CREATE" ->
                        em.remove(
                                em.find(
                                        resolveEntity(action.getEntityName()),
                                        action.getEntityId()
                                )
                        );

                case "UPDATE", "DELETE" ->
                        em.merge(
                                mapper.readValue(
                                        action.getBeforeState(),
                                        resolveEntity(action.getEntityName())
                                )
                        );
            }
        }
    }

    private Class<?> resolveEntity(String entityName) {
        return switch (entityName) {
            case "ITEM" -> Item.class;
            default ->
                    throw new IllegalArgumentException(
                            "Unknown entity: " + entityName
                    );
        };
    }
}
