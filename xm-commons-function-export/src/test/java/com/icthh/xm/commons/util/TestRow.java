package com.icthh.xm.commons.util;

import com.icthh.xm.commons.domain.BaseRow;
import lombok.AllArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

import java.util.List;

@AllArgsConstructor
public class TestRow implements BaseRow {

    private Long id;
    private String name;
    private int age;

    @JsonIgnore
    @Override
    public List<String> getHeaders() {
        return List.of("ID", "NAME", "AGE");
    }

    @JsonIgnore
    @Override
    public List<Object> getFieldValues() {
        return List.of(id, name, age);
    }
}
