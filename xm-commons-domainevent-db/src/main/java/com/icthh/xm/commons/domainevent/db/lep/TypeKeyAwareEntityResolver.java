package com.icthh.xm.commons.domainevent.db.lep;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.icthh.xm.commons.domainevent.db.service.mapper.impl.TypeKeyAwareJpaEntityMapper.TYPE_KEY;

@Component
public class TypeKeyAwareEntityResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        JpaEntityContext jpaEntityContext = method.getParameter("jpaEntityContext", JpaEntityContext.class);
        String typeKey = jpaEntityContext.findPropertyStateValue(TYPE_KEY);
        return List.of(typeKey.toUpperCase());
    }
}
