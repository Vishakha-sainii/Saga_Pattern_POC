package com.example.saga.events;

public record BillingUpdateFailedEvent(
        Long itemId,
        String oldName
) {}
