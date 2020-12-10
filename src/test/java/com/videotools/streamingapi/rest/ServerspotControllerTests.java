package com.videotools.streamingapi.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;


@SpringBootTest
@AutoConfigureMockMvc
public class ServerspotControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void noParamServerspotsShouldReturnList() throws Exception {

        this.mockMvc.perform(
                get("/serverspots"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.serverspotList[0].name")
                            .value("Bilbo Serverspot"))
                    .andExpect(jsonPath("$._embedded.serverspotList[1].name")
                            .value("Frodo Serverspot"));
    }

    @Test
    public void idParamServerspotsShouldReturnSingleEmp() throws Exception {

        // Failure hint: depending on the other databases
        // the id might not be 1 & 2, but 3 & 4, or other
        this.mockMvc.perform(
                get("/serverspots/{id}", 5))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name")
                            .value("Bilbo Serverspot"));
    }

}
