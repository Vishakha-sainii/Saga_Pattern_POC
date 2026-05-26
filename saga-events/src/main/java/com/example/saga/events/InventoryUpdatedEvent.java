package com.example.saga.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public record InventoryUpdatedEvent(
        Long itemId,
        String oldName
) {}
