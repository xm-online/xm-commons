package com.icthh.xm.commons.service.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.domain.BaseRow;
import com.icthh.xm.commons.service.FunctionExecutorService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CvsExportService<T extends BaseRow> extends AbstractExportServiceImpl<T> {

    private static final CsvMapper csvMapper = buildCsvMapper();

    protected CvsExportService(FunctionExecutorService functionExecutorService) {
        super(functionExecutorService);
    }

    @Override
    public boolean supports(String fileFormat) {
        return "csv".equalsIgnoreCase(fileFormat);
    }

    @Override
    public void export(String functionKey, String fileFormat, Map<String, Object> functionInput, HttpServletResponse response) {
        log.info("Start export function key [{}], file format [{}]", functionKey, fileFormat);

        try (OutputStream os = response.getOutputStream()) {

            int page = 0;
            Page<T> result = getNextPage(page, functionKey, functionInput);
            ObjectWriter csvWriter = createCsvWriterWithHeaders(result.getContent());

            try (SequenceWriter seqWriter = csvWriter.writeValues(os)) {
                while (true) {
                    writePageContent(seqWriter, result);
                    if (!result.hasNext()) {
                        break;
                    }
                    result = getNextPage(++page, functionKey, functionInput);
                }
            }

        } catch (Exception e) {
            log.error("Exception while exporting data to csv file, {}", e.getMessage());
            throw new IllegalStateException("Exception while exporting data to csv file", e);
        }
    }

    private void writePageContent(SequenceWriter seqWriter, Page<T> page) throws IOException {
        for (T item : page.getContent()) {
            seqWriter.write(item.getFieldValues());
        }
    }

    private ObjectWriter createCsvWriterWithHeaders(List<T> exportData) {
        List<String> headers = Optional.ofNullable(exportData.getFirst().getHeaders()).orElse(List.of());

        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        headers.forEach(schemaBuilder::addColumn);
        CsvSchema schema = schemaBuilder.build().withHeader();

        return csvMapper.writer(schema);
    }

    private static CsvMapper buildCsvMapper() {
        CsvMapper mapper = new CsvMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
