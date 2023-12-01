package com.icthh.xm.commons.lep.groovy.storage;

import java.net.URLConnection;

public interface LepConnectionCache {
    URLConnection putConnection(String url, LepConnectionProvider connection);
    boolean isConnectionExists(String url);
    URLConnection getConnection(String url);
}
