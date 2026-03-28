package com.zhouri.farmshop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StorefrontControllerReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reviewEndpointWithAuthIsAcceptedWithoutCsrf() throws Exception {
        String token = """
                eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJjb25zdW1lciIsInVzZXJJZCI6MSwiZnVsbE5hbWUiOiLmma7pgJrmtojotLnogIUiLCJyb2xlIjoiQ09OU1VNRVIiLCJpYXQiOjE3NzQ2NjQ4NjQsImV4cCI6MTc3NDc1MTI2NH0.aybqZdVdOetqDAaC9GV44f8lpCICsmaTi29t7ghDnpQsouPoDTn16-6kVh2-c8VD
                """.trim();

        mockMvc.perform(post("/api/catalog/products/1/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"orderItemId":45,"rating":5,"content":"integration review test"}
                                """))
                .andExpect(status().isOk());
    }
}
