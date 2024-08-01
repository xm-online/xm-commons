package com.icthh.xm.commons.search.query.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IndexQuery {
    private String id;
    private String type;
    private String indexName;
    private Object object;
}
