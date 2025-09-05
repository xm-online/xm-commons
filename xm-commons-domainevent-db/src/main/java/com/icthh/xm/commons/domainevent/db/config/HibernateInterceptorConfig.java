package com.icthh.xm.commons.domainevent.db.config;

import com.icthh.xm.commons.domainevent.db.service.DatabaseSourceInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.util.HashMap;

/**
 * Forces Hibernate to use our custom Interceptor in the SessionFactory.
 * <p>
 * Why it is needed:
 * <ul>
 *   <li>By default, Hibernate may end up with {@code EmptyInterceptor} due to bootstrapping order.</li>
 *   <li>This BeanPostProcessor ensures that {@link AvailableSettings#INTERCEPTOR} is always set
 *       to our {@link Interceptor} bean before the EntityManagerFactory is initialized.</li>
 * </ul>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "application.domain-event.enabled", havingValue = "true")
public class HibernateInterceptorConfig {

    /**
     * BeanPostProcessor that overrides the JPA property map of the
     * {@link LocalContainerEntityManagerFactoryBean} to insert our Interceptor.
     *
     * @param interceptor the custom Hibernate Interceptor bean
     * @return BeanPostProcessor that enforces the Interceptor in the EMF
     */
    @Bean
    public BeanPostProcessor forceSfInterceptorBpp(@Lazy DatabaseSourceInterceptor interceptor) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String name) {
                if (bean instanceof AbstractEntityManagerFactoryBean emf) {
                    var jpa = new HashMap<>(emf.getJpaPropertyMap());
                    jpa.put(AvailableSettings.INTERCEPTOR, interceptor);
                    emf.setJpaPropertyMap(jpa);

                    log.info("Applied Hibernate Interceptor [{}] (id={}) to EMF bean '{}'",
                        interceptor.getClass().getName(), System.identityHashCode(interceptor), name);
                }
                return bean;
            }
        };
    }
}
