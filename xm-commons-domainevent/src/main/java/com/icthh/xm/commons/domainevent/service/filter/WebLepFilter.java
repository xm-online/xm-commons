package com.icthh.xm.commons.domainevent.service.filter;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.commons.domainevent.keyresolver.FilterKeyResolver;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import org.springframework.stereotype.Service;

@Service
@LepService(group = "filter", name = "default")
public class WebLepFilter {

    @LogicExtensionPoint(value = "WebFilter", resolver = FilterKeyResolver.class)
    public boolean lepFiltering(String key, DomainEvent domainEvent) {
        return false;
    }
}
