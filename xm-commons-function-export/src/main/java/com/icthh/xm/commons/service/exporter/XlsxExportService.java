package com.icthh.xm.commons.service.exporter;

import com.icthh.xm.commons.domain.BaseRow;
import com.icthh.xm.commons.service.FunctionExecutorService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.IntStream.range;

@Slf4j
@Component
public class XlsxExportService<T extends BaseRow> extends AbstractExportServiceImpl<T> {

    private static final String SHEET_NAME = "Report";

    protected XlsxExportService(FunctionExecutorService functionExecutorService) {
        super(functionExecutorService);
    }

    @Override
    public boolean supports(String fileFormat) {
        return Set.of("xlsx", "xls").contains(fileFormat);
    }

    @Override
    public void export(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response) {
        int page = 0;
        long rowIndex = 0;

        Page<T> result = getNextPage(page, functionKey, functionInput);

        // SXSSFWorkbook: streaming workbook (keep only 100 rows in memory at a time)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ServletOutputStream os = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            writeHeaderRow(sheet, result.getContent());
            rowIndex++;
            while (true) {

                rowIndex = writeDataRows(sheet, result.getContent(), rowIndex);
                if (!result.hasNext()) {
                    break;
                }
                result = getNextPage(++page, functionKey, functionInput);
            }

            workbook.write(os);
            os.flush();

        } catch (IOException e) {
            log.error("Exception while exporting data to xlsx file, {}", e.getMessage());
            throw new IllegalStateException("Exception while exporting data to xlsx file", e);
        }
    }

    private void writeHeaderRow(Sheet sheet, List<T> firstRow) {
        List<String> headers = Optional.ofNullable(firstRow)
            .filter(data -> !data.isEmpty())
            .map(d -> d.getFirst().getHeaders())
            .orElse(List.of());

        fillRow(sheet.createRow(0), headers);
    }

    private long writeDataRows(Sheet sheet, List<T> rows, long startIndex) {
        long rowIndex = startIndex;

        for (T item : rows) {
            Row row = sheet.createRow((int) rowIndex++);
            fillRow(row, item.getFieldValues());
        }

        return rowIndex;
    }

    private void fillRow(Row row, List<?> rowValues) {
        range(0, rowValues.size())
            .forEach(index -> {
                String value = Optional.ofNullable(rowValues.get(index)).map(Object::toString).orElse("");
                row.createCell(index).setCellValue(value);
            });
    }
}
