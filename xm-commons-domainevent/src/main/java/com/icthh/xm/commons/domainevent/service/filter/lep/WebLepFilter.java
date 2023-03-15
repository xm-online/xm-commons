package com.icthh.xm.commons.domainevent.service.filter.lep;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.keyresolver.DomainEventFilterKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.springframework.stereotype.Service;

@Service
@LepService(group = "filter")
public class WebLepFilter {

    @LogicExtensionPoint(value = "WebFilter", resolver = DomainEventFilterKeyResolver.class)
    public boolean lepFiltering(String key, DomainEvent domainEvent) {
        return false;
    }
}
