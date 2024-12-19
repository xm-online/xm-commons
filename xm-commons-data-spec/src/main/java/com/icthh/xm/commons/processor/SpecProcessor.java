package com.icthh.xm.commons.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.domain.DataSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class SpecProcessor<S extends DataSpec> implements ISpecProcessor<S> {

    protected static final String REF = "$ref";
    protected static final String KEY = "key";
    protected static final String FORM_KEY = "formKey";
    protected final AntPathMatcher matcher;
    protected final ObjectMapper ymlMapper;
    protected final ObjectMapper jsonMapper;
    protected final JsonListenerService jsonListenerService;

    public SpecProcessor(JsonListenerService jsonListenerService) {
        this.jsonListenerService = jsonListenerService;
        this.matcher = new AntPathMatcher();
        this.jsonMapper = new ObjectMapper();
        this.ymlMapper = new ObjectMapper(new YAMLFactory());
    }

    public abstract String getSectionName();
    public abstract String getReferencePattern();
    public abstract String getKeyTemplate();

    @SneakyThrows
    protected Set<String> findDataSpecReferencesByPattern(String dataSpec, String refPattern) {
        return new ObjectMapper().readTree(dataSpec)
            .findValuesAsText(REF)
            .stream()
            .filter(value -> matcher.matchStart(refPattern, value))
            .collect(Collectors.toSet());
    }

    public Map<String, S> toKeyMapOverrideDuplicates(Collection<S> definitionSpecs) {
        return Optional.ofNullable(definitionSpecs)
            .orElseGet(List::of)
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(S::getKey, Function.identity(), (key1, key2) -> key1, LinkedHashMap::new));
    }

    public Map<String, S> toKeyMapStrict(Collection<S> definitionSpecs) {
        return Optional.ofNullable(definitionSpecs)
            .orElseGet(List::of)
            .stream()
            .collect(toMap(S::getKey, Function.identity(), this::handleDuplicateKey, LinkedHashMap::new));
    }

    private S handleDuplicateKey(S u, S v) {
        log.warn("Duplicate key found: {}", u);
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }
}
