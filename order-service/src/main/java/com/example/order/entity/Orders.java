package com.example.order.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long inventoryId;
    private String status;


    public Orders(Long inventoryId) {
    }

}
