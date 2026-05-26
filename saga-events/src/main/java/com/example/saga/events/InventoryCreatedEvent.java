package com.example.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryCreatedEvent {
    private Long itemId;
    private String name;
    private int quantity;
}
