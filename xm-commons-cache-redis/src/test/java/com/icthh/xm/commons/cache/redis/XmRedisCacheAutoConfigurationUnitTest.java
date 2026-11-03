package com.icthh.xm.commons.cache.redis;

import com.icthh.xm.commons.cache.redis.XmRedisCacheAutoConfiguration.XmRedisCacheProperties;
import org.junit.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XmRedisCacheAutoConfigurationUnitTest {

    private final XmRedisCacheAutoConfiguration configuration = new XmRedisCacheAutoConfiguration();

    @Test
    public void usernameAndSslAreApplied() {
        XmRedisCacheProperties props = new XmRedisCacheProperties();
        props.setHost("redis");
        props.setPort(16379);
        props.setUsername("user");
        props.setPassword("secret");
        props.setDatabase(2);
        props.setSsl(true);

        LettuceConnectionFactory factory = (LettuceConnectionFactory) configuration.xmRedisConnectionFactory(props);

        RedisStandaloneConfiguration cfg = factory.getStandaloneConfiguration();
        assertEquals("redis", cfg.getHostName());
        assertEquals(16379, cfg.getPort());
        assertEquals("user", cfg.getUsername());
        assertEquals("secret", new String(cfg.getPassword().get()));
        assertEquals(2, cfg.getDatabase());
        assertTrue(factory.isUseSsl());
        factory.destroy();
    }

    @Test
    public void emptyUsernameAndSslAreNotApplied() {
        XmRedisCacheProperties props = new XmRedisCacheProperties();
        props.setUsername("");
        props.setPassword("");

        LettuceConnectionFactory factory = (LettuceConnectionFactory) configuration.xmRedisConnectionFactory(props);

        RedisStandaloneConfiguration cfg = factory.getStandaloneConfiguration();
        assertEquals("localhost", cfg.getHostName());
        assertEquals(6379, cfg.getPort());
        assertNull(cfg.getUsername());
        assertFalse(cfg.getPassword().isPresent());
        assertFalse(factory.isUseSsl());
        factory.destroy();
    }
}
