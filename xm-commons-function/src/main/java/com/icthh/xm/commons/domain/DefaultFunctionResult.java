package com.icthh.xm.commons.domain;

import static com.icthh.xm.commons.utils.ModelAndViewUtils.MVC_FUNC_RESULT;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.MILLIS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.servlet.ModelAndView;

@Data
@NoArgsConstructor
public class DefaultFunctionResult implements FunctionResult {

    private long executeTime;
    private Object data;

    @JsonIgnore
    @Transient
    @Setter
    private transient Boolean wrapResult;

    public DefaultFunctionResult(Object data, Boolean wrapResult) {
        this.data = data;
        this.wrapResult = wrapResult;
    }

    public DefaultFunctionResult(Object data) {
        this.data = data;
    }

    @JsonIgnore
    @Override
    public Object functionResult() {
        if (TRUE.equals(wrapResult)) {
            return this;
        }
        return this.data;
    }

    @JsonIgnore
    @Override
    public ModelAndView getModelAndView() {
        return (ModelAndView) Optional.ofNullable(getData())
            .flatMap(isInstanceOf(Map.class))
            .map(d -> d.get(MVC_FUNC_RESULT))
            .orElse(null);
    }

    private <T> Function<Object, Optional<T>> isInstanceOf(Class<T> type) {
        return obj -> type.isInstance(obj) ? Optional.of((T) obj) : Optional.empty();
    }
}
