package com.icthh.xm.commons.search.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

@Builder
@Getter
public class SearchDto {

    private String query;
    private Pageable pageable;
    private ElasticFetchSourceFilterDto fetchSourceFilter;
    private Class entityClass;
}
