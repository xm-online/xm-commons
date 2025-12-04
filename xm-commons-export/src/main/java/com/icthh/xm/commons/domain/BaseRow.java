package com.icthh.xm.commons.domain;

import java.util.List;

public interface BaseRow {

    List<String> getHeaders();

    List<Object> getFieldValues();
}
