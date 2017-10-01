package com.icthh.xm.commons.config.client.config;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bitsofinfo.hazelcast.discovery.consul.BaseRegistrator;
import org.springframework.cloud.commons.util.InetUtils;

import java.util.Map;

@Slf4j
public class CustomDiscoveryNodeRegistrator extends BaseRegistrator {

    /**
     * properties that are supported in the JSON value for the 'consul-registrator-config' config property
     * in ADDITION to those defined in BaseRegistrator
     */
    public static final String CONFIG_PROP_PREFER_PUBLIC_ADDRESS = "preferPublicAddress";

    @Override
    @SneakyThrows
    public Address determineMyLocalAddress(DiscoveryNode localDiscoveryNode, Map<String, Object> registratorConfig) {

        Address myLocalAddress = localDiscoveryNode.getPrivateAddress();

        Object usePublicAddress = registratorConfig.get(CONFIG_PROP_PREFER_PUBLIC_ADDRESS);
        if (usePublicAddress != null && usePublicAddress instanceof Boolean && (Boolean) usePublicAddress) {
            log.info("Registrator config property: {}:{} attempting to use it...", CONFIG_PROP_PREFER_PUBLIC_ADDRESS, usePublicAddress);
            Address publicAddress = localDiscoveryNode.getPublicAddress();
            myLocalAddress = publicAddress != null ? publicAddress : myLocalAddress;
        }

        return new Address(InetUtils.getFirstNonLoopbackHostInfo().getIpAddress(), myLocalAddress.getPort());
    }


}
