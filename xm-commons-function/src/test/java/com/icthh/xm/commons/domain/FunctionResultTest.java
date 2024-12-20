package com.icthh.xm.commons.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.utils.ModelAndViewUtils.MVC_FUNC_RESULT;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunctionResultTest implements FunctionResult {

    private Long id;
    private String key;
    private Instant startDate;
    private Instant endDate;
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();
    private transient boolean onlyData;

    @Override
    public Map<String, Object> getData() {
        return this.data;
    }

    @Override
    public long getExecuteTime() {
        Instant startDate = getStartDate();
        Instant endDate = getEndDate();
        return startDate != null && endDate != null ? Duration.between(startDate, endDate).getSeconds() : 0;
    }

    @Override
    public Object functionResult() {
        return this.data;
    }

    @Override
    public ModelAndView getModelAndView() {
        return (ModelAndView) Optional.ofNullable(getData())
            .map(d -> d.get(MVC_FUNC_RESULT))
            .orElse(null);
    }

    public FunctionResultTest data(Map<String, Object> data) {
        this.data = data;
        return this;
    }
}
