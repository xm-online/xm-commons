package com.icthh.xm.commons.timeline.util;

import com.jayway.jsonpath.JsonPath;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@UtilityClass
public class JsonUtils {

    public static Object findField(String entity, String field, List<String> prefixes) {
        if (CollectionUtils.isEmpty(prefixes)) {
            return "";
        }

        for (String prefix : prefixes) {
            try {
                return JsonPath.read(entity, prefix + field);
            } catch (Exception ex) {
                log.trace("JsonPath exception", ex);
            }
        }

        return "";
    }

}
