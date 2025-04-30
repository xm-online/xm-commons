package com.icthh.xm.commons.permission.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
public class PermissionContextDto implements Serializable {

    private List<String> permissions;
    private Map<String, Object> ctx;
    private Integer hash;

    public PermissionContextDto() {
        this.permissions = List.of();
        this.ctx = Map.of();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PermissionContextDto that = (PermissionContextDto) o;
        return Objects.equals(permissions, that.permissions) && Objects.equals(ctx, that.ctx);
    }

    @Override
    public int hashCode() {
        if (hash == null) {
            hash = Objects.hash(permissions, ctx);
        }
        return hash;
    }
}
