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

    private final Map<String, Item> itemsMap = new HashMap<>();

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(new ArrayList<>(itemsMap.values()));
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        itemsMap.put(item.getId(), item);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }
}
