package com.icthh.xm.commons.domainevent.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpDomainEventPayload extends DomainEventPayload {
    private String method;
    private String url;
    private String urlPattern;
    private Map<String, Object> urlPathParams;
    private String queryString;
    private Long requestLength;
    private String requestBody;
    private Long responseLength;
    private String responseBody;
    private Map<String, List<String>> requestHeaders;
    private Map<String, List<String>> responseHeaders;
    private Integer responseCode;
    private Long execTime;
}
