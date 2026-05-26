package com.example.saga.sagaCommon.steps;

import com.example.saga.sagaCommon.SagaStep;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

public class JpaCreateStep<T> implements SagaStep {

    private final JpaRepository<T, Long> repo;
    @Getter
    private T entity;

    public JpaCreateStep(JpaRepository<T, Long> repo, T entity) {
        this.repo = repo;
        this.entity = entity;
    }

    @Override
    public void execute() {
        entity = repo.save(entity);
    }

    @Override
    public void rollback() {
        repo.delete(entity);
    }

}

