package com.example.inventory.service;

import com.example.inventory.entity.SagaAction;
import com.example.inventory.repository.SagaActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SagaLogger {

    private final SagaActionRepository repo;
    private final ObjectMapper mapper;

    // CREATE
    public void logCreate(String sagaId, String entityName, Long entityId) {
        repo.save(new SagaAction(
                null, sagaId, "CREATE", entityName, entityId, null
        ));
    }

    // UPDATE
    public void logUpdate(
            String sagaId,
            String entityName,
            Long entityId,
            Object beforeState
    ) {
        try {
            repo.save(new SagaAction(
                    null,
                    sagaId,
                    "UPDATE",
                    entityName,
                    entityId,
                    mapper.writeValueAsString(beforeState)
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE
    public void logDelete(
            String sagaId,
            String entityName,
            Long entityId,
            Object beforeState
    ) {
        try {
            repo.save(new SagaAction(
                    null,
                    sagaId,
                    "DELETE",
                    entityName,
                    entityId,
                    mapper.writeValueAsString(beforeState)
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ROLLBACK
    public void logRollback(
            String sagaId,
            String entityName,
            Long entityId
    ) {
        repo.save(new SagaAction(
                null,
                sagaId,
                "ROLLBACK",
                entityName,
                entityId,
                null
        ));
    }
}

