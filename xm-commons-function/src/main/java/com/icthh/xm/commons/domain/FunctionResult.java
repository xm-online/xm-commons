package com.icthh.xm.commons.domain;

import com.icthh.xm.commons.logging.util.MdcUtils;
import org.springframework.web.servlet.ModelAndView;

public interface FunctionResult {

    /**
     * Get function result object related id
     */
    default Long getId() {
        return null;
    }

    /**
     * Get function execution related data
     */
    Object getData();

    /**
     * Get function execution time
     */
    long getExecuteTime();

    /**
     * Get function execution rid
     */
    default String getRid() {
        return MdcUtils.getRid();
    }

    /**
     * Get function execution result
     */
    Object functionResult();

    /**
     * Get ModelAndView from data if provided
     * @return  ModelAndView
     */
    default ModelAndView getModelAndView() {
        return null;
    }
}
