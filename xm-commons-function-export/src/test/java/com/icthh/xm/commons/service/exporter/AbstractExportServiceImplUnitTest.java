package com.icthh.xm.commons.service.exporter;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.service.FunctionExecutorService;
import com.icthh.xm.commons.util.TestExportService;
import com.icthh.xm.commons.util.TestRow;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractExportServiceImplUnitTest {

    private FunctionExecutorService executorService;

    private TestExportService service;

    @BeforeEach
    void setUp() {
        executorService = mock(FunctionExecutorService.class);
        service = new TestExportService(executorService);
        service.defaultPageSize = 1000;
    }

    @Test
    void testGetNextPage_whenResultIsList() {
        List<TestRow> rows = List.of(new TestRow(1L, "A", 20), new TestRow(2L, "B", 30));

        when(executorService.execute(eq("key"), anyMap(), eq(HttpMethod.GET.name()))).thenReturn(rows);

        Map<String, Object> input = new HashMap<>();
        Page<TestRow> page = service.getNextPage(2, "key", input);

        assertEquals(2, page.getContent().size());
        assertEquals(List.of(1L, "A", 20), page.getContent().get(0).getFieldValues());

        assertEquals(2, input.get("page"));
        assertEquals(1000, input.get("size"));
    }

    @Test
    void testGetNextPage_whenResultIsPage() {
        Page<TestRow> expectedPage = new PageImpl<>(List.of(new TestRow(1L, "A", 20)));

        when(executorService.execute(eq("key"), anyMap(), eq(HttpMethod.GET.name()))).thenReturn(expectedPage);

        Page<TestRow> page = service.getNextPage(0, "key", new HashMap<>());

        assertSame(expectedPage, page);
        assertEquals(List.of(1L, "A", 20), page.getContent().get(0).getFieldValues());
    }

    @Test
    void testGetNextPage_whenResultIsNull() {
        when(executorService.execute(eq("key"), anyMap(), eq(HttpMethod.GET.name()))).thenReturn(null);

        assertThrows(BusinessException.class,
            () -> service.getNextPage(1, "key", new HashMap<>()),
            "Empty export data for file; functionKey: key; page: 1"
        );
    }

    @Test
    void testGetNextPage_whenUnexpectedType() {
        when(executorService.execute(eq("key"), anyMap(), eq(HttpMethod.GET.name()))).thenReturn("some string");

        assertThrows(BusinessException.class,
            () -> service.getNextPage(1, "key", new HashMap<>()),
            "Unexpected function result: String. List or Page are expected."
        );
    }

    @Test
    void testGetNextPage_setsPageAndSize() {
        when(executorService.execute(eq("key"), anyMap(), eq(HttpMethod.GET.name()))).thenReturn(Collections.emptyList());

        Map<String, Object> input = new HashMap<>();

        service.getNextPage(5, "key", input);

        assertEquals(5, input.get("page"));
        assertEquals(1000, input.get("size"));
    }

    public static ServletOutputStream mockServletOutputStream(ByteArrayOutputStream outputStream) {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) {
                outputStream.write(b);
            }
        };
    }

    public static ArgumentMatcher<Map<String, Object>> withPage(Integer page) {
        return actual -> actual != null
            && actual.size() >= 2
            && actual.containsKey("page")
            && actual.containsKey("size")
            && actual.get("page").equals(page);
    }
}
