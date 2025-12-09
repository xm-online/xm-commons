package com.icthh.xm.commons.service.exporter;

import com.icthh.xm.commons.domain.BaseRow;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.service.FunctionExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractExportServiceImpl<T extends BaseRow> implements ExportService {

    @Value("${application.function.export.defaultPageSize:1000}")
    protected Integer defaultPageSize;

    private final FunctionExecutorService functionExecutorService;

    public AbstractExportServiceImpl(FunctionExecutorService functionExecutorService) {
        this.functionExecutorService = functionExecutorService;
    }

    protected Page<T> getNextPage(Integer page, String functionKey, Map<String, Object> functionInput) {
        functionInput.put("page", page);
        functionInput.put("size", defaultPageSize);

        Object result = functionExecutorService.execute(functionKey, functionInput, HttpMethod.GET.name());

        if (result == null) {
            log.error("Empty export data for file");
            throw new BusinessException("Empty export data for file; functionKey: " + functionKey + "; page: " + page);
        }

        if (result instanceof List<?>) {
            List<T> resultList = (List<T>) result;
            return new PageImpl<T>(resultList, Pageable.unpaged(), resultList.size());
        }

        if (result instanceof Page<?>) {
            return (Page<T>) result;
        }
        log.error("Unexpected function result type: {}",  result.getClass());
        throw new BusinessException("Unexpected function result: {}. List or Page are expected.", result.getClass().getName());
    }
}
