package com.example.inventory.saga;

import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "order-service",
        url = "http://localhost:9091",
        path = "/orders"
)
public interface OrderClient {

    @PostMapping("/create")
    void createOrder(@RequestParam("inventoryId") Long inventoryId,
                     @RequestParam("sagaId") String sagaId);

    @DeleteMapping("/rollback/{inventoryId}")
    void rollbackCreate(@PathVariable Long inventoryId);

    @PutMapping("/{inventoryId}")
    void updateOrder(@PathVariable Long inventoryId);

    @PutMapping("/rollback/{inventoryId}")
    void rollbackUpdate(@PathVariable Long inventoryId);

    @DeleteMapping("/{inventoryId}")
    void deleteOrder(@PathVariable Long inventoryId);

    @PostMapping("/rollback-delete/{inventoryId}")
    void rollbackDelete(@PathVariable Long inventoryId);
}

