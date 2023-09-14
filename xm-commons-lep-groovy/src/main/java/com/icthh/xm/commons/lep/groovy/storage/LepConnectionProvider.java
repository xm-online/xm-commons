package com.icthh.xm.commons.lep.groovy.storage;

import groovy.util.ResourceException;

import java.net.URLConnection;

@FunctionalInterface
public interface LepConnectionProvider {
    URLConnection get() throws ResourceException;
}
