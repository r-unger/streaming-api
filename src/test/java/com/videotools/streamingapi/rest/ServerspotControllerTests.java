package com.videotools.streamingapi.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videotools.streamingapi.model.Serverspot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ServerspotControllerTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    public void getAllShouldBeForbidden() throws Exception {

        this.mockMvc.perform(
                get("/serverspots"))
                .andDo(print())
                .andExpect(status().isForbidden());
        // if a list of all items should be returned, then
        // this.mockMvc.perform(
        // get("/serverspots"))
        // .andDo(print())
        // .andExpect(status().isOk())
        // .andExpect(jsonPath("$._embedded.serverspotList[0].group")
        // .value("abc"))
        // .andExpect(jsonPath("$._embedded.serverspotList[1].group")
        // .value("xyz"));
    }

    @Test
    public void getShouldBeForbidden() throws Exception {

        this.mockMvc.perform(
                get("/serverspots/{id}", 1234))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    public void postShouldReturnANewSpot() throws Exception {

        this.mockMvc.perform(
                post("/serverspots"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    public void putShouldBeForbidden() throws Exception {

        this.mockMvc.perform(
                put("/serverspots/{id}", 1234)
                        .content(mapper.writeValueAsString(
                                new Serverspot(
                                        "www1.thecompany.com",
                                        "live.eftv",
                                        1234)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    public void deleteShouldBeForbidden() throws Exception {

        this.mockMvc.perform(
                delete("/serverspots/{id}", 1234))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

}
