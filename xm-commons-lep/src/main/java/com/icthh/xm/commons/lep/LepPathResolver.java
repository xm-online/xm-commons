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
            baseLepPaths.stream().map(it -> it.lepFolderPrefix.apply(tenantKey))
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

        String relativeLepPath = lepPath;
        return this.reversedBaseLepPaths.stream()
            .filter(it -> it.isMatchPrefix(relativeLepPath))
            .map(it -> it.buildAbsoluteLepPath(tenant, relativeLepPath))
            .findFirst().get(); // safe, because isMatchPrefix always true in microservice LepPath
    }

    public List<String> getLepCommonsPaths(String tenant) {
        return baseLepPaths.stream()
            .map(it -> it.getCommonsPath(tenant))
            .collect(toList());
    }

    public List<String> getLepPathPatterns() {
        return baseLepPaths.stream()
            .map(LepPath::getLepFolderAntPattern)
            .collect(toList());
    }

    public List<LepRootPath> getLepPathVariants(String tenantKey) {
        List<String> parentKeys = tenantAliasService.getTenantAliasTree()
            .getParentKeys(tenantKey);

        return baseLepPaths.stream()
            .flatMap(it -> it.buildLepRootPaths(tenantKey, parentKeys).stream())
            .collect(toList());
    }

    @RequiredArgsConstructor
    public static class LepPath {
        @Getter
        private final String lepFolderAntPattern;
        // path to folder in config repository and prefix on import lep class
        private final Function<String, String> lepFolderPrefix;
        // prefix that we get using call tenant or env commons
        private final String lepPrefix;
        private final String relativeCommonsFolderPath;

        private final AntPathMatcher pathMatcher = new AntPathMatcher();

        public String getCommonsPath(String tenant) {
            return lepFolderPrefix.apply(tenant) + relativeCommonsFolderPath;
        }

        public boolean isMatchPatter(String path) {
            return pathMatcher.match(lepFolderAntPattern, path);
        }


        public String getPathVariable(String path, String variable) {
            return pathMatcher.extractUriTemplateVariables(lepFolderAntPattern, path).get(variable);
        }

        public boolean isMatchPrefix(String lepPath) {
            return lepPath.startsWith(lepPrefix);
        }

        public String buildAbsoluteLepPath(String tenant, String relativeLepPath) {
            String lepPath = relativeLepPath.substring(lepPrefix.length());
            if (!lepPath.startsWith("/")) {
                lepPath = "/" + lepPath;
            }
            return lepFolderPrefix.apply(tenant) + lepPath;
        }

        public List<LepRootPath> buildLepRootPaths(String tenantKey, List<String> parentTenants) {
            List<LepRootPath> result = new ArrayList<>();
            result.add(new LepRootPath(lepFolderPrefix.apply(tenantKey), lepFolderPrefix.apply(tenantKey)));

            if (StringUtils.isNotBlank(lepPrefix)) {
                result.add(new LepRootPath(lepPrefix, lepFolderPrefix.apply(tenantKey)));
            }

            for (var parentTenant: parentTenants) {
                LepRootPath lepRootPath = new LepRootPath(lepFolderPrefix.apply(parentTenant), lepFolderPrefix.apply(tenantKey));
                result.add(lepRootPath);
            }

            return result;
        }
    }

    @ToString
    @RequiredArgsConstructor
    public static class LepRootPath {
        private final String prefix;
        private final String rootPath;

        public String getPath(String name) {
            return rootPath + name.substring(prefix.length());
        }

        public boolean isMatch(String name) {
            return name.startsWith(prefix);
        }

    }

}
