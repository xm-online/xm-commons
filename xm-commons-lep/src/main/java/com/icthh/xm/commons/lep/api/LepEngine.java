package com.icthh.xm.commons.lep.api;

import com.icthh.xm.commons.lep.ProceedingLep;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class LepEngine {

    @Getter
    private final String id;
    private final List<DestroyCallback> destroyCallbacks = new ArrayList<>();

    protected LepEngine() {
        this(UUID.randomUUID().toString());
    }

    protected LepEngine(String id) {
        this.id = id;
    }

    public int order() {
        return 0;
    }

    public abstract boolean isExists(LepKey lepKey);

    public abstract Object invoke(LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext);

    public void addDestroyCallback(DestroyCallback destroyCallback) {
        destroyCallbacks.add(destroyCallback);
    }

    public void destroy() {
        destroyCallbacks.forEach(it -> it.onDestroy(this));
    }

    public interface DestroyCallback {
        void onDestroy(LepEngine lepEngine);
    }
}
