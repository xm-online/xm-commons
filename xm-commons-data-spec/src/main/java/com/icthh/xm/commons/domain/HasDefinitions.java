package com.icthh.xm.commons.domain;

import java.util.List;

public interface HasDefinitions {

    default List<DefinitionSpec> getDefinitions() {
        return null; // null to ignore in json
    }

}
