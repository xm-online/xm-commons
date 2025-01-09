package com.icthh.xm.commons.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.ModelAndViewUtils.MVC_FUNC_RESULT;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultFunctionResult implements FunctionResult {

    private Instant startDate;
    private Instant endDate;
    private Map<String, Object> data = new HashMap<>();

    @Override
    public long getExecuteTime() {
        Instant startDate = Optional.ofNullable(this.startDate).orElse(Instant.now());
        Instant endDate = Optional.ofNullable(this.endDate).orElse(Instant.now());
        return startDate != null && endDate != null ? Duration.between(startDate, endDate).getSeconds() : 0;
    }

    @JsonIgnore
    @Override
    public Object functionResult() {
        return this.data;
    }

    @JsonIgnore
    @Override
    public ModelAndView getModelAndView() {
        return (ModelAndView) Optional.ofNullable(getData())
            .map(d -> d.get(MVC_FUNC_RESULT))
            .orElse(null);
    }
}
