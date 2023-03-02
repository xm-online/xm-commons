package com.icthh.xm.commons.domainevent.db.config;

import com.icthh.xm.commons.domainevent.db.service.DatabaseSourceInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DatabaseSourceInterceptorCustomizer implements HibernatePropertiesCustomizer {

    public static final String SESSION_FACTORY_INTERCEPTOR_PROPERTY = "hibernate.session_factory.interceptor";

    private final DatabaseSourceInterceptor databaseSourceInterceptor;

    public DatabaseSourceInterceptorCustomizer(@Lazy DatabaseSourceInterceptor databaseSourceInterceptor) {
        this.databaseSourceInterceptor = databaseSourceInterceptor;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(SESSION_FACTORY_INTERCEPTOR_PROPERTY, databaseSourceInterceptor);
        log.info("DatabaseSourceInterceptor is enabled");
    }
}
