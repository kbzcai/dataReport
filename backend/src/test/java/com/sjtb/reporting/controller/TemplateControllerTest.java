package com.sjtb.reporting.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sjtb.reporting.service.ExcelService;
import com.sjtb.reporting.service.TemplateService;
import com.sjtb.reporting.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
class TemplateControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private TemplateService templates;
    @MockBean private ExcelService excel;
    @MockBean private JwtService jwtService;

    @Test
    void importSampleUsesItsStaticRouteInsteadOfTheTemplateIdRoute() throws Exception {
        when(excel.templateImportSample()).thenReturn(new byte[] {1, 2, 3});

        mvc.perform(get("/api/templates/import-sample"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }
}
