package com.icthh.xm.commons.search.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static java.util.Optional.ofNullable;

@Getter
@Setter
public class ElasticFetchSourceFilterDto {

    private List<String> includes;
    private List<String> excludes;

    public String[] getIncludes() {
        return toArray(includes);
    }

    public String[] getExcludes() {
        return toArray(excludes);
    }

    private String[] toArray(List<String> excludes) {
        return ofNullable(excludes)
            .map(list -> list.toArray(String[]::new))
            .orElse(null);
    }
}
