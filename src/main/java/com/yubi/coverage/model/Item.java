package com.yubi.coverage.model;

import java.util.UUID;

public class Item {
    private String id;
    private String name;
    private String description;

    public Item() {
        this.id = UUID.randomUUID().toString();
    }

    public Item(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
