package com.sjtb.reporting.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sjtb.reporting.security.JwtService;
import com.sjtb.reporting.service.LateFillRequestService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LateFillRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class LateFillRequestControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private LateFillRequestService service;
    @MockBean private JwtService jwtService;

    @Test
    void listRouteIsMappedInTheCurrentApplicationBuild() throws Exception {
        when(service.list()).thenReturn(List.of());

        mvc.perform(get("/api/late-fill-requests"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
