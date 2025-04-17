package com.yubi.coverage.controller;

import com.yubi.coverage.model.Item;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    // Using HashMap to store data for experimental purposes
    private final Map<String, Item> itemsMap = new HashMap<>();

    // Index - Get all items
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(new ArrayList<>(itemsMap.values()));
    }

    // Show - Get a specific item by ID
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable String id) {
        Item item = itemsMap.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // Create - Add a new item
    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        itemsMap.put(item.getId(), item);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    // Update - Update an existing item
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable String id, @RequestBody Item itemDetails) {
        Item item = itemsMap.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        
        item.setName(itemDetails.getName());
        item.setDescription(itemDetails.getDescription());
        
        itemsMap.put(id, item);
        return ResponseEntity.ok(item);
    }

    // Delete - Remove an item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        if (!itemsMap.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        
        itemsMap.remove(id);
        return ResponseEntity.noContent().build();
    }
}
