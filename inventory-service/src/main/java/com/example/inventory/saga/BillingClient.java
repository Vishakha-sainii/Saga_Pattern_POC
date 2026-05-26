package com.example.inventory.saga;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BillingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "http://billing-service/billing";

    public void createBill(Long inventoryId) {
        restTemplate.postForObject(BASE_URL, inventoryId, Void.class);
    }

    public void rollbackCreate(Long inventoryId) {
        restTemplate.delete(BASE_URL + "/rollback/" + inventoryId);
    }

    public void updateBill(Long inventoryId) {
        restTemplate.put(BASE_URL + "/" + inventoryId, null);
    }

    public void rollbackUpdate(Long inventoryId) {
        restTemplate.put(BASE_URL + "/rollback/" + inventoryId, null);
    }

    public void deleteBill(Long inventoryId) {
        restTemplate.delete(BASE_URL + "/" + inventoryId);
    }

    public void rollbackDelete(Long inventoryId) {
        restTemplate.postForObject(
                BASE_URL + "/rollback-delete/" + inventoryId,
                null,
                Void.class
        );
    }
}
