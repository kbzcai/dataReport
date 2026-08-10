package com.sjtb.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class ExcelServiceTest {
    @Test
    void templateImportSampleContainsOneHeaderOnlySheet() throws Exception {
        ExcelService service = new ExcelService(null, null, null, new ObjectMapper());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(service.templateImportSample()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            assertThat(workbook.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) 3);
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
    }
}
