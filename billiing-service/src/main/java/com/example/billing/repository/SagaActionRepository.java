package com.example.billing.repository;

import com.example.billing.entity.SagaAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SagaActionRepository extends JpaRepository<SagaAction, Long> {
    List<SagaAction> findBySagaIdOrderByIdDesc(String sagaId);
}
