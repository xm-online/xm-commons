package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceService;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/flow/resources")
@RequiredArgsConstructor
public class TenantResourceResource {

    private final TenantResourceService resourceService;

    @GetMapping("/{resourceKey}")
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FLOW.RESOURCE.GET_ITEM')")
    @PrivilegeDescription("Privilege to get the resource by resourceKey")
    public TenantResource getResource(@PathVariable("resourceKey") String resourceKey) {
        return resourceService.getResource(resourceKey);
    }

    @GetMapping()
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FLOW.RESOURCE.GET_LIST')")
    @PrivilegeDescription("Privilege to get all resources")
    public List<TenantResource> getResources(@RequestParam(name = "resourceType", required = false) String resourceType) {
        return resourceService.getResources(resourceType);
    }

    @PostMapping()
    @PreAuthorize("hasPermission({'resource': #resource}, 'FLOW.RESOURCE.CREATE')")
    @PrivilegeDescription("Privilege to create a new resource")
    public void createResource(@RequestBody TenantResource resource) {
        resourceService.createResource(resource);
    }

    @PutMapping()
    @PreAuthorize("hasPermission({'resource': #resource}, 'FLOW.RESOURCE.UPDATE')")
    @PrivilegeDescription("Privilege to update the resource")
    public void updateResource(@RequestBody TenantResource resource) {
        resourceService.updateResource(resource);
    }

    @DeleteMapping("/{resourceKey}")
    @PreAuthorize("hasPermission({'resourceKey': #resourceKey}, 'FLOW.RESOURCE.DELETE')")
    @PrivilegeDescription("Privilege to delete the resource")
    public void deleteResource(@PathVariable("resourceKey") String resourceKey) {
        resourceService.deleteResource(resourceKey);
    }

}
