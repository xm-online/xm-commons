package com.icthh.xm.commons.lep.groovy.storage;

import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;

import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmptyLepConnectionCache implements LepConnectionCache {

    private final Map<String, LepConnectionProvider> resolvedConnections = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public URLConnection putConnection(String url, LepConnectionProvider connection) {
        return connection.get();
    }

    @Override
    public boolean isConnectionExists(String url) {
        return false;
    }

    @Override
    @SneakyThrows
    public URLConnection getConnection(String url) {
        throw new NotImplementedException("Method not supported");
    }
}
