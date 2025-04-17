package com.yubi.coverage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubi.coverage.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Item testItem;

    @BeforeEach
    void setUp() throws Exception {
        testItem = new Item("TestName", "TestDescription");
        // Create an item for show/update/delete tests
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testItem)));
    }

    @Test
    void testGetAllItems() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetItemById() throws Exception {
        mockMvc.perform(get("/api/items/" + testItem.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testItem.getId())))
                .andExpect(jsonPath("$.name", is(testItem.getName())))
                .andExpect(jsonPath("$.description", is(testItem.getDescription())));
    }

    @Test
    void testCreateItem() throws Exception {
        Item newItem = new Item("NewName", "NewDescription");
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("NewName")))
                .andExpect(jsonPath("$.description", is("NewDescription")));
    }

    @Test
    void testUpdateItem() throws Exception {
        Item updated = new Item("UpdatedName", "UpdatedDescription");
        updated.setId(testItem.getId());
        mockMvc.perform(put("/api/items/" + testItem.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testItem.getId())))
                .andExpect(jsonPath("$.name", is("UpdatedName")))
                .andExpect(jsonPath("$.description", is("UpdatedDescription")));
    }

    @Test
    void testDeleteItem() throws Exception {
        mockMvc.perform(delete("/api/items/" + testItem.getId()))
                .andExpect(status().isNoContent());
        // Should return 404 after deletion
        mockMvc.perform(get("/api/items/" + testItem.getId()))
                .andExpect(status().isNotFound());
    }
}
