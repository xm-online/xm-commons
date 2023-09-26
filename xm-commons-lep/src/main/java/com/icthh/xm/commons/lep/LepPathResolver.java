package com.icthh.xm.commons.lep;

import com.icthh.xm.commons.config.client.service.TenantAliasService;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.spring.ApplicationNameProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.reverse;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class LepPathResolver {

    public static final String URL_PREFIX_COMMONS_ENVIRONMENT = "commons/environment";
    public static final String URL_PREFIX_COMMONS_TENANT = "commons/tenant";
    private static final String TENANT_NAME = "tenantKey";
    public static final String ENV_COMMONS = "commons";


    private final String appName;
    private final TenantAliasService tenantAliasService;
    private final List<LepPath> baseLepPaths;
    private final List<LepPath> reversedBaseLepPaths;

    public LepPathResolver(ApplicationNameProvider applicationNameProvider, TenantAliasService tenantAliasService) {
        this.appName = applicationNameProvider.getAppName();
        this.tenantAliasService = tenantAliasService;
        this.baseLepPaths = buildLepPaths();
        this.reversedBaseLepPaths = new ArrayList<>(baseLepPaths);
        reverse(this.reversedBaseLepPaths);
    }

    private List<LepPath> buildLepPaths() {
        return List.of(
            new LepPath(
                "/config/tenants/{tenantKey}/" + appName + "/lep/**",
                tenant -> tenant + "/" + appName + "/lep",
                "",
                "/commons/"
            ),
            new LepPath(
                "/config/tenants/{tenantKey}/commons/lep/**",
                tenant -> tenant + "/commons/lep",
                URL_PREFIX_COMMONS_TENANT,
                "/"
            ),
            new LepPath(
                "/config/tenants/commons/lep/**",
                tenant -> "commons/lep",
                URL_PREFIX_COMMONS_ENVIRONMENT,
                "/"
            )
        );
    }

    public String getTenantFromPath(String path) {
        return baseLepPaths.stream()
            .filter(it -> it.isMatchPatter(path))
            .map(it -> it.getPathVariable(path, TENANT_NAME))
            .filter(Objects::nonNull)
            .findAny().orElse(ENV_COMMONS);
    }

    public List<String> getLepBasePaths(String tenant) {
        List<String> tenantKeys = new ArrayList<>();
        tenantKeys.add(tenant);
        tenantKeys.addAll(tenantAliasService.getTenantAliasTree().getParentKeys(tenant));

        return tenantKeys.stream().flatMap(tenantKey ->
            baseLepPaths.stream().map(it -> it.folderPrefix.apply(tenantKey))
        ).collect(toList());
    }

    public String getLepPath(LepKey lepKey, String tenant) {
        return buildLepPath(lepKey, tenant, identity());
    }

    public String getLegacyLepPath(LepKey lepKey, String tenant) {
        return buildLepPath(lepKey, tenant, LepPathResolver::translateToLepConvention);
    }

    private static String translateToLepConvention(String xmEntitySpecKey) {
        return xmEntitySpecKey.replaceAll("-", "_").replaceAll("\\.", "\\$");
    }

    private String buildLepPath(LepKey lepKey, String tenant, Function<String, String> segmentMapper) {
        String lepPath = lepKey.getBaseKey();
        List<String> segments = lepKey.getSegments();
        if (StringUtils.isNotBlank(lepKey.getGroup())) {
            lepPath = lepKey.getGroup().replace(".", "/") + "/" + lepKey.getBaseKey();
        }
        if (isNotEmpty(segments)) {
            segments = segments.stream().map(segmentMapper).collect(toList());
            lepPath = lepPath + "$$" + StringUtils.join(segments, "$$");
        }

        String resultLepPath = lepPath;
        return this.reversedBaseLepPaths.stream()
            .filter(it -> it.isMatchPrefix(resultLepPath))
            .map(it -> it.buildLepPath(tenant, resultLepPath))
            .findFirst().get();
    }

    public List<String> getLepCommonsPaths(String tenant) {
        return baseLepPaths.stream()
            .map(it -> it.getCommonsPath(tenant))
            .collect(toList());
    }

    public List<String> getLepPathPatterns() {
        return baseLepPaths.stream()
            .map(LepPath::getAntFolderPattern)
            .collect(toList());
    }


    public Optional<LepRootPath> getLepBasePath(String tenantKey, String name) {
        List<LepRootPath> rootPathVariants = new ArrayList<>();

        rootPathVariants.add(new LepRootPath(name, tenantKey + "/" + appName, tenantKey + "/" + appName));

        List<String> parentKeys = tenantAliasService.getTenantAliasTree()
            .getParentKeys(tenantKey);
        parentKeys.stream()
            .map(tenant -> tenant + "/" + appName)
            .map(it -> new LepRootPath(name, it, tenantKey + "/" + appName))
            .forEach(rootPathVariants::add);

        parentKeys.stream()
            .map(tenant -> tenant + "/commons")
            .map(it -> new LepRootPath(name, it, tenantKey + "/commons"))
            .forEach(rootPathVariants::add);

        rootPathVariants.add(new LepRootPath(name, URL_PREFIX_COMMONS_TENANT, tenantKey + "/commons/lep"));
        rootPathVariants.add(new LepRootPath(name, tenantKey + "/commons", tenantKey + "/commons"));
        rootPathVariants.add(new LepRootPath(name, URL_PREFIX_COMMONS_ENVIRONMENT, "commons/lep"));
        rootPathVariants.add(new LepRootPath(name, "commons", "commons"));

        return rootPathVariants.stream().filter(LepRootPath::isMatch).findFirst();
    }

    @RequiredArgsConstructor
    public static class LepPath {
        @Getter
        private final String antFolderPattern;
        private final Function<String, String> folderPrefix;
        private final String lepPrefix;
        private final String commonsFolder;

        private final AntPathMatcher pathMatcher = new AntPathMatcher();

        public String getCommonsPath(String tenant) {
            return folderPrefix.apply(tenant) + commonsFolder;
        }

        public boolean isMatchPatter(String path) {
            return pathMatcher.match(antFolderPattern, path);
        }


        public String getPathVariable(String path, String variable) {
            return pathMatcher.extractUriTemplateVariables(antFolderPattern, path).get(variable);
        }

        public boolean isMatchPrefix(String lepPath) {
            return lepPath.startsWith(lepPrefix);
        }

        public String buildLepPath(String tenant, String resultLepPath) {
            String lepPath = resultLepPath.substring(lepPrefix.length());
            if (!lepPath.startsWith("/")) {
                lepPath = "/" + lepPath;
            }
            return folderPrefix.apply(tenant) + lepPath;
        }
    }

    @ToString
    @RequiredArgsConstructor
    public static class LepRootPath {
        private final String name;
        private final String prefix;
        private final String rootPath;

        public String getPath() {
            return rootPath + name.substring(prefix.length());
        }

        public boolean isMatch() {
            return name.startsWith(prefix);
        }

    }

}
