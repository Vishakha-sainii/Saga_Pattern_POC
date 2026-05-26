package com.example.saga.sagaCommon.steps;

import com.example.saga.sagaCommon.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

public class JpaUpdateStep<T> implements SagaStep {

    private final JpaRepository<T, Long> repo;
    private final Long id;
    private T oldState;
    private T newState;

    public JpaUpdateStep(JpaRepository<T, Long> repo, Long id, T newState) {
        this.repo = repo;
        this.id = id;
        this.newState = newState;
    }

    public void execute() {
        oldState = repo.findById(id).orElseThrow();
        repo.save(newState);
    }

    public void rollback() {
        repo.save(oldState);
    }

}
