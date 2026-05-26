package com.example.order.repository;

import com.example.order.entity.SagaAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaActionRepository extends JpaRepository<SagaAction, Long> {
    List<SagaAction> findBySagaIdOrderByIdDesc(String sagaId);
}
