package com.icthh.xm.commons.lep.groovy.storage;

import lombok.SneakyThrows;

import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryLepConnectionCache implements LepConnectionCache {

    private final Map<String, LepConnectionProvider> resolvedConnections = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public URLConnection putConnection(String url, LepConnectionProvider connection) {
        resolvedConnections.put(url, connection);
        return connection.get();
    }

    @Override
    public boolean isConnectionExists(String url) {
        return resolvedConnections.containsKey(url);
    }

    @Override
    @SneakyThrows
    public URLConnection getConnection(String url) {
        return resolvedConnections.get(url).get();
    }
}
