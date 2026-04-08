package com.icthh.xm.commons.cache.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Auto-configures the Redis cache strategy for {@code xm-commons-cache}.
 *
 * Activated only when {@code application.tenant-cache.redis.enabled=true}.
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(value = "application.tenant-cache.redis.enabled", havingValue = "true")
public class XmRedisCacheAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "application.tenant-cache.redis")
    public XmRedisCacheProperties xmRedisCacheProperties() {
        return new XmRedisCacheProperties();
    }

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory xmRedisConnectionFactory(XmRedisCacheProperties props) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
        if (props.getPassword() != null && !props.getPassword().isEmpty()) {
            cfg.setPassword(props.getPassword());
        }
        cfg.setDatabase(props.getDatabase());
        LettuceConnectionFactory factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    public DynamicRedisCacheManager dynamicRedisCacheManager(RedisConnectionFactory connectionFactory) {
        return new DynamicRedisCacheManager(connectionFactory);
    }

    @Getter
    @Setter
    public static class XmRedisCacheProperties {
        private boolean enabled;
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
    }
}
