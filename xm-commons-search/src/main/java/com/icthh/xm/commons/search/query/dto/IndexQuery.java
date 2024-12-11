package com.icthh.xm.commons.search.query.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IndexQuery {
    private String id;
    private Object object;
    private String indexName;
    private String type;
    private String source;
    private String parentId;
}
