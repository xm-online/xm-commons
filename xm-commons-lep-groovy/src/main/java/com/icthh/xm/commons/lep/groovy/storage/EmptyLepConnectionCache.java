package com.icthh.xm.commons.lep.groovy.storage;

import lombok.SneakyThrows;
import org.apache.commons.lang3.NotImplementedException;

import java.net.URLConnection;

public class EmptyLepConnectionCache implements LepConnectionCache {

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
