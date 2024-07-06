package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.dto.Flow;
import com.icthh.xm.commons.flow.domain.dto.Step;
import com.icthh.xm.commons.flow.domain.dto.Step.Snippet;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map.Entry;

import static com.icthh.xm.commons.config.client.utils.ListUtils.nullSafeList;
import static java.util.stream.Collectors.toList;

@Component
@LepService(group = "flow")
public class CodeSnippetService {

    private final TenantContextHolder tenantContextHolder;
    private final String appName;

    public CodeSnippetService(@Value("${spring.application.name}") String appName, TenantContextHolder tenantContextHolder) {
        this.appName = appName;
        this.tenantContextHolder = tenantContextHolder;
    }

    @LogicExtensionPoint(value = "GenerateSnippet")
    public List<Configuration> generateSnippets(Flow flow) {
        List<Step> steps = flow.getSteps();
        return nullSafeList(steps).stream()
            .map(Step::getSnippets)
            .flatMap(it -> it.entrySet().stream())
            .map(this::toConfiguration)
            .collect(toList());
    }

    private Configuration toConfiguration(Entry<String, Snippet> it) {
        String content = "";
        if (it.getValue() != null) {
            content = it.getValue().getContent();
        }
        return new Configuration(buildPath(it), content);
    }

    private String buildPath(Entry<String, Snippet> it) {
        String fileName = it.getKey() + "." + it.getValue().getExtension();
        if (fileName.contains("/")) {
            throw new BusinessException("error.illegal.code.snippet.file.name", "File name can't contain '/' character");
        }
        String tenantKey = tenantContextHolder.getTenantKey().toUpperCase();
        String folderPath = "/config/tenants/" + tenantKey + "/" + appName + "/flow/snippets/";
        return folderPath + fileName;
    }
}
