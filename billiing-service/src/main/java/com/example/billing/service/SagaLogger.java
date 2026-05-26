package com.example.billing.service;

import com.example.billing.entity.SagaAction;
import com.example.billing.repository.SagaActionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SagaLogger {

    private final SagaActionRepository repo;
    private final ObjectMapper mapper;

    // CREATE
    public void logCreate(
            String sagaId,
            String entityName,
            Long entityId
    ) {
        repo.save(new SagaAction(
                null,
                sagaId,
                "CREATE",
                entityName,
                entityId,
                null
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

    public String getBeforeState(String sagaId, String entityName, Long entityId) {
        try {
            // Find all actions for this SAGA, ordered by most recent first
            List<SagaAction> actions = repo.findBySagaIdOrderByIdDesc(sagaId);

            // Look for UPDATE or DELETE operation for the specific entity
            for (SagaAction action : actions) {
                if (action.getEntityName().equals(entityName)
                        && action.getEntityId().equals(entityId)
                        && ("UPDATE".equals(action.getOperation()) || "DELETE".equals(action.getOperation()))
                        && action.getBeforeState() != null) {

                    return action.getBeforeState();
                }
            }

            // No before state found
            return null;

        } catch (Exception ex) {
            throw new RuntimeException("Failed to retrieve before state for SAGA: " + sagaId, ex);
        }
    }
}
