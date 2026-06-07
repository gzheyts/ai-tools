package com.example.demo;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class CreateTests {

        @Test
        void create_validInput_returnsCreated() throws Exception {
            // given
            var requestBody = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john.doe@example.com"
                }
                """;

            // when / then
            mockMvc.perform(post("/api/v1/persons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.id").exists());
        }

        @Test
        void create_missingFirstName_returnsBadRequest() throws Exception {
            // given
            var requestBody = """
                {
                    "lastName": "Doe",
                    "email": "john.doe@example.com"
                }
                """;

            // when / then
            mockMvc.perform(post("/api/v1/persons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void findById_nonExistentId_returnsNotFoundOrError() throws Exception {
            // given
            var nonExistentId = "00000000-0000-0000-0000-000000000001";

            // when / then
            mockMvc.perform(get("/api/v1/persons/{id}", nonExistentId))
                .andExpect(status().is5xxServerError());
        }
    }
}
