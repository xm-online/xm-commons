package com.icthh.xm.commons.domain;

import java.util.List;

public interface HasForms {

    default List<FormSpec> getForms() {
        return null; // null to ignore in json
    }

}
