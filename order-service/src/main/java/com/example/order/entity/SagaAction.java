package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saga_actions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SagaAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;

    private String operation;     // CREATE, UPDATE, DELETE
    private String entityName;    // INVENTORY, ORDER, BILLING
    private Long entityId;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

}

