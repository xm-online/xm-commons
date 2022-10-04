package com.icthh.xm.commons.lep;

import java.util.Map;

public interface CacheableLepEngine {

    void clearCache(Map<String, XmLepScriptResource> partToUpdate);

}
