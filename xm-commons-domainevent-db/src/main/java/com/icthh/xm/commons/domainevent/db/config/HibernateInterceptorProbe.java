package com.icthh.xm.commons.domainevent.db.config;

import com.icthh.xm.commons.domainevent.db.service.DatabaseSourceInterceptor;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Diagnostic probe that verifies which Interceptor is actually installed
 * in the Hibernate SessionFactory at runtime.
 * <p>
 * Why it is needed:
 * <ul>
 *   <li>Helps ensure our {@link Interceptor} bean is the same instance
 *       used by the SessionFactory.</li>
 *   <li>Detects misconfiguration or cases where Hibernate falls back
 *       to {@code EmptyInterceptor}.</li>
 *   <li>Can optionally fail-fast on mismatch using a property flag.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "application.domain-event.enabled", havingValue = "true")
class HibernateInterceptorProbe implements ApplicationRunner {

    private final EntityManagerFactory emf;

    @Value("${application.domain-event.fail-on-interceptor-mismatch:false}")
    private boolean failOnMismatch;

    @Override
    public void run(ApplicationArguments args) {
        var sfi = emf.unwrap(SessionFactoryImplementor.class);
        var sfInterceptor = sfi.getSessionFactoryOptions().getInterceptor();

        boolean sameType = sfInterceptor instanceof DatabaseSourceInterceptor;

        log.info("Hibernate Interceptor check: SF={}#{} sameType={}",
            sfInterceptor.getClass().getSimpleName(), System.identityHashCode(sfInterceptor), sameType);

        if (!sameType) {
            var msg = "Hibernate Interceptor in SessionFactory != domain event interceptor";
            if (failOnMismatch) {
                throw new IllegalStateException(msg);
            } else {
                log.warn("{} (set application.domain-event.fail-on-interceptor-mismatch=true to fail-fast)", msg);
            }
        }
    }
}
