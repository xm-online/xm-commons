package com.icthh.xm.commons.flow.service;

import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.flow.domain.Flow;
import com.icthh.xm.commons.flow.domain.Step;
import com.icthh.xm.commons.flow.domain.Step.Snippet;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.icthh.xm.commons.config.client.utils.Utils.nullSafeList;
import static com.icthh.xm.commons.config.client.utils.Utils.nullSafeMap;
import static com.icthh.xm.commons.flow.service.CodeSnippetExecutor.SNIPPET;
import static java.util.stream.Collectors.toList;

@Component
@LepService(group = "flow")
public class CodeSnippetService {

    public static final String SEGMENT_SEP = "$$";
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
            .flatMap(step -> toConfigurations(flow, step).stream())
            .collect(toList());
    }

    private List<Configuration> toConfigurations(Flow flow, Step step) {
        return nullSafeMap(step.getSnippets()).entrySet().stream()
            .map((entry) ->
                toConfiguration(flow.getKey(), step.getKey(), entry.getKey(), entry.getValue())
            ).collect(toList());
    }

    private Configuration toConfiguration(String flowKey, String stepKey, String fileKey, Snippet snippet) {
        String content = "";
        if (snippet != null) {
            content = snippet.getContent();
        }
        return new Configuration(buildPath(flowKey, stepKey, fileKey, snippet), content);
    }

    private String buildPath(String flowKey, String stepKey, String fileKey, Snippet snippet) {
        String fileName = SNIPPET + SEGMENT_SEP + flowKey + SEGMENT_SEP + stepKey + SEGMENT_SEP + fileKey + "." + snippet.getExtension();
        if (fileName.contains("/")) {
            throw new BusinessException("error.illegal.code.snippet.file.name", "File name can't contain '/' character");
        }
        String tenantKey = tenantContextHolder.getTenantKey().toUpperCase();
        String folderPath = "/config/tenants/" + tenantKey + "/" + appName + "/flow/snippets/";
        return folderPath + fileName;
    }
}
