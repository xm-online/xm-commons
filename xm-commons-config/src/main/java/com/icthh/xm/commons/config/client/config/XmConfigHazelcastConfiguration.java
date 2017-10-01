package com.icthh.xm.commons.config.client.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty("xm-config.enabled")
public class XmConfigHazelcastConfiguration {

    public static final String HAZELCAST_CONFIG_URL_PROPERTY = "hazelcast-config-url";
    public static final String TENANT_CONFIGURATION_MAP = "tenant-configuration";
    public static final String TENANT_CONFIGURATION_HAZELCAST = "tenant-configuration-hazelcast";
    public static final String INTERFACES = "interfaces";
    public static final String HAZELCAST_LOCAL_LOCAL_ADDRESS = "hazelcast.local.localAddress";

    private final XmConfigProperties appProps;

    private final ApplicationContext context;

    @Bean(TENANT_CONFIGURATION_HAZELCAST)
    public HazelcastInstance tenantConfigurationHazelcast() throws IOException {
        log.info("{}", appProps.getHazelcast());

        Properties props = new Properties();
        props.putAll(appProps.getHazelcast());
        props.put(HAZELCAST_LOCAL_LOCAL_ADDRESS, InetUtils.getFirstNonLoopbackHostInfo().getIpAddress());

        String hazelcastConfigUrl = appProps.getHazelcast().get(HAZELCAST_CONFIG_URL_PROPERTY);
        InputStream in = context.getResource(hazelcastConfigUrl).getInputStream();

        Config config = new XmlConfigBuilder(in).setProperties(props).build();
        config.getNetworkConfig().setInterfaces(buildInterfaces(appProps.getHazelcast().get(INTERFACES)));
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        return hazelcastInstance;
    }

    private InterfacesConfig buildInterfaces(String interfaces) {
        InterfacesConfig interfacesConfig = new InterfacesConfig();
        interfacesConfig.setEnabled(Boolean.TRUE);

        if (interfaces == null || interfaces.trim().isEmpty()) {
            log.warn("Hazelcast interfaces list is empty");
            return interfacesConfig;
        }

        for (String ip : interfaces.split(",")) {
            if (ip != null && !ip.trim().isEmpty()) {
                interfacesConfig.addInterface(ip);
            }
        }

        return interfacesConfig;
    }

    @PreDestroy
    public void destroy() {
        log.info("Closing hazelcast");
        Hazelcast.shutdownAll();
    }

}
