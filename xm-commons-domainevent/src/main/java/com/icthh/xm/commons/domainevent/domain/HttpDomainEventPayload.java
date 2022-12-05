package com.icthh.xm.commons.domainevent.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.http.HttpHeaders;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpDomainEventPayload extends DomainEventPayload {
    private String method;
    private String url;
    private String queryString;
    private Long requestLength;
    private String requestBody;
    private Long responseLength;
    private String responseBody;
    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private Integer responseCode;
    private Long execTime;
}
