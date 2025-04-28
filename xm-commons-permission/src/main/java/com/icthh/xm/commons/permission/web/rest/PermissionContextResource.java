package com.icthh.xm.commons.permission.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.domain.dto.PermissionContextDto;
import com.icthh.xm.commons.permission.service.PermissionContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/permission/context")
public class PermissionContextResource {

    private final PermissionContextService permissionContextService;

    @GetMapping()
    @PreAuthorize("hasPermission({'userKey': #userKey}, 'PERMISSION.CONTEXT.GET')")
    @PrivilegeDescription("Privilege to call get permission context")
    public ResponseEntity<PermissionContextDto> callGetFunction(@RequestParam String userKey) {
        PermissionContextDto context = permissionContextService.getPermissionContext(userKey);
        return ResponseEntity.ok().body(context);
    }
}
