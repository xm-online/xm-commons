package com.icthh.xm.commons.service.exporter;

import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.util.TestRow;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.icthh.xm.commons.service.exporter.AbstractExportServiceImplUnitTest.mockServletOutputStream;
import static com.icthh.xm.commons.service.exporter.AbstractExportServiceImplUnitTest.withPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


public class XlsxExportServiceUnitTest {

    private static final String FUNCTION_KEY = "functionKey";

    @Mock
    private FunctionExecutorService functionExecutorService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private XlsxExportService<TestRow> xlsxExportService;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        outputStream = new ByteArrayOutputStream();
        ServletOutputStream servletOut = mockServletOutputStream(outputStream);

        when(response.getOutputStream()).thenReturn(servletOut);
    }

    @Test
    public void testExportMultiPageXlsx() throws Exception {
        Page<TestRow> page0 = new PageImpl<>(
            List.of(new TestRow(1L, "Alice", 10), new TestRow(2L, "Bob", 20)),
            PageRequest.of(0, 2),
            3
        );

        Page<TestRow> page1 = new PageImpl<>(
            List.of(new TestRow(3L, "Charlie", 30)),
            PageRequest.of(1, 2),
            3
        );

        when(functionExecutorService.execute(eq(FUNCTION_KEY), argThat(withPage(0)), eq(HttpMethod.GET.name()))).thenReturn(page0);
        when(functionExecutorService.execute(eq(FUNCTION_KEY), argThat(withPage(1)), eq(HttpMethod.GET.name()))).thenReturn(page1);

        xlsxExportService.export(FUNCTION_KEY, "xlsx", new HashMap<>(), response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = wb.getSheet("Report");

            assertRow(sheet.getRow(0), "ID", "NAME", "AGE");
            assertRow(sheet.getRow(1), "1", "Alice", "10");
            assertRow(sheet.getRow(2), "2", "Bob", "20");
            assertRow(sheet.getRow(3), "3", "Charlie", "30");

            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(4);
        }
    }

    @Test
    public void testExportEmptyPageXlsx() throws Exception {
        Page<TestRow> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 2), 0);

        when(functionExecutorService.execute(eq(FUNCTION_KEY), argThat(withPage(0)), eq(HttpMethod.GET.name()))).thenReturn(emptyPage);

        xlsxExportService.export(FUNCTION_KEY, "xlsx", new HashMap<>(), response);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()))) {
            Sheet sheet = wb.getSheet("Report");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
        }
    }

    private void assertRow(Row row, String... expected) {
        for (int i = 0; i < expected.length; i++) {
            assertThat(row.getCell(i).getStringCellValue()).isEqualTo(expected[i]);
        }
    }
}
