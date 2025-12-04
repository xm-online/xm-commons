package com.icthh.xm.commons.service.exporter;

import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExportServiceProvider {

    private final Set<ExportService> exportServices;

    public ExportService getExportService(String format) {
        return exportServices.stream()
            .filter(s -> s.supports(format))
            .findFirst()
            .orElseThrow(() -> new BusinessException("File format not supported: " + format));
    }
}
