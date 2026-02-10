package net.noworks.clionesql.boot.samples;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void findAll() throws Exception {
        mockMvc.perform(get("/persons")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].NAME").value("Alice")).andExpect(jsonPath("$[1].NAME").value("Bob"));
    }

    @Test
    void findByName() throws Exception {
        mockMvc.perform(get("/persons/search").param("name", "Alice")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].NAME").value("Alice"))
                .andExpect(jsonPath("$[1].NAME").value("Alice"));
    }

    @Test
    void findByNameBlankReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/persons/search").param("name", "")).andExpect(status().isBadRequest());
    }

    @Test
    void findByNameNoMatch() throws Exception {
        mockMvc.perform(get("/persons/search").param("name", "Charlie")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findById() throws Exception {
        mockMvc.perform(get("/persons/1")).andExpect(status().isOk()).andExpect(jsonPath("$.NAME").value("Alice"))
                .andExpect(jsonPath("$.STATUS").value("ACTIVE"));
    }

    @Test
    void insert() throws Exception {
        mockMvc.perform(post("/persons").param("id", "10").param("name", "Dave").param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1));

        mockMvc.perform(get("/persons/10")).andExpect(status().isOk()).andExpect(jsonPath("$.NAME").value("Dave"));
    }

    @Test
    void updateStatus() throws Exception {
        mockMvc.perform(put("/persons/1/status").param("status", "INACTIVE")).andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        mockMvc.perform(get("/persons/search").param("name", "Alice")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].STATUS").value("INACTIVE"))
                .andExpect(jsonPath("$[1].STATUS").value("ACTIVE"));
    }
}
