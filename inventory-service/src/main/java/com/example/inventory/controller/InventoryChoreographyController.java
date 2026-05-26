package com.example.inventory.controller;

import com.example.inventory.service.InventoryChoreographyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory-choreo")
public class InventoryChoreographyController {

    @Autowired
    InventoryChoreographyService inventoryChoreographyService;

    @PostMapping("/create")
    public ResponseEntity<String> create() {
        inventoryChoreographyService.createItem("RedTape Shoes", 1);
        return ResponseEntity.ok("Inventory created & event published");
    }

    @PutMapping("/update")
    public void updateItem(@RequestParam Long itemId,
                           @RequestParam String newName) {
        inventoryChoreographyService.updateItem(itemId, newName);
    }
}
