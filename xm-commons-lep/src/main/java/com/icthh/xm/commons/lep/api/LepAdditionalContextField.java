package com.icthh.xm.commons.lep.api;

public interface LepAdditionalContextField {

    default Object get(String fieldName) {
        return null; // implementation in BaseLepContext
    }

}
