package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface DomainEventProvider {

    DomainEvent createEvent(HttpServletRequest request, HttpServletResponse response, String tenant,
                            String clientId, String userKey, String[] aggregateDetails, String responseBody);

}
