package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DomainEventProvider {

    DomainEvent createEvent(HttpServletRequest request, HttpServletResponse response, String tenant,
                            String clientId, String userKey, String[] aggregateDetails, String responseBody);

}
