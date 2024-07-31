package com.icthh.xm.commons.lep.groovy.storage;

import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class DirtyClassPathConfigLepStorage implements LepStorage {
    private final Map<String, XmLepConfigFile> leps = new ConcurrentHashMap<>();

    @Override
    public void forEach(Consumer<XmLepConfigFile> action) {
        // do nothing
    }

    @Override
    public LepConnectionCache buildCache() {
        return new EmptyLepConnectionCache();
    }

    @Override
    public XmLepConfigFile getByPath(String path) {
        return leps.get(path);
    }

    @Override
    public boolean isExists(String path) {
        return leps.containsKey(path);
    }

    public void update(Map<String, XmLepConfigFile> leps) {
        var keys = new HashSet<>(this.leps.keySet());
        keys.removeAll(leps.keySet());
        keys.forEach(this.leps::remove);

        leps.forEach((key, value) -> {
            if (!equalContent(this.leps.get(key), value)) {
                this.leps.put(key, value);
            }
        });
    }

    @SneakyThrows
    private boolean equalContent(XmLepConfigFile oldConfig, XmLepConfigFile newConfig) {
        byte[] oldValue = getValue(oldConfig);
        byte[] newValue = getValue(newConfig);
        return Arrays.equals(oldValue, newValue);
    }


    private static byte[] getValue(XmLepConfigFile firstValue) {
        return ofNullable(firstValue)
            .map(XmLepConfigFile::getContentStream)
            .map(rethrow(InputStreamSource::getInputStream))
            .map(rethrow(IOUtils::toByteArray))
            .orElse(new byte[0]);
    }

    private static <I, O> Function<I, O> rethrow(FunctionWithException<I, O> value) {
        return (i) -> runFunction(value, i);
    }

    @SneakyThrows
    private static <I, O> O runFunction(FunctionWithException<I, O> value, I i) {
        return value.apply(i);
    }

    private interface FunctionWithException<I, O> {
        O apply(I i) throws Exception;
    }
}
