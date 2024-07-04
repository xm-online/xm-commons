package com.icthh.xm.commons.flow.rest;

import com.icthh.xm.commons.flow.domain.TenantResource;
import com.icthh.xm.commons.flow.service.TenantResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/flow/resources")
@RequiredArgsConstructor
public class TenantResourceResource {

    private final TenantResourceService resourceService;

    @GetMapping("/{resourceKey}")
    public TenantResource getResource(@PathVariable("resourceKey") String resourceKey) {
        return resourceService.getResource(resourceKey);
    }

    @GetMapping()
    public List<TenantResource> getResources(@RequestParam(name = "resourceType", required = false) String resourceType) {
        return resourceService.getResources(resourceType);
    }

    @PostMapping()
    public void createResource(@RequestBody TenantResource resource) {
        resourceService.createResource(resource);
    }

    @PutMapping()
    public void updateResource(@RequestBody TenantResource resource) {
        resourceService.updateResource(resource);
    }

    @DeleteMapping("/{resourceKey}")
    public void deleteResource(@PathVariable("resourceKey") String resourceKey) {
        resourceService.deleteResource(resourceKey);
    }

}
