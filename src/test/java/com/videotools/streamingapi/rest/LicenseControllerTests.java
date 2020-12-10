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
public class LicenseControllerTests {
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void noParamLicensesShouldReturnList() throws Exception {

        this.mockMvc.perform(
                get("/licenses"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.licenseList[0].name")
                            .value("Bilbo License"))
                    .andExpect(jsonPath("$._embedded.licenseList[1].name")
                            .value("Frodo License"));
    }

    @Test
    public void idParamLicensesShouldReturnSingleEmp() throws Exception {

        // Failure hint: depending on the other databases
        // the id might not be 1 & 2, but 3 & 4, or other
        this.mockMvc.perform(
                get("/licenses/{id}", 3))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name")
                            .value("Bilbo License"));
    }

}
