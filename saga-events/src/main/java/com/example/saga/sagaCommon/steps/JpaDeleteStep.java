package com.example.saga.sagaCommon.steps;

import com.example.saga.sagaCommon.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

public class JpaDeleteStep<T> implements SagaStep {

    private final JpaRepository<T, Long> repo;
    private final Long id;
    private T deleted;

    public JpaDeleteStep(JpaRepository<T, Long> repo, Long id) {
        this.repo = repo;
        this.id = id;
    }

    public void execute() {
        deleted = repo.findById(id).orElseThrow();
        repo.delete(deleted);
    }

    public void rollback() {
        repo.save(deleted);
    }
}
