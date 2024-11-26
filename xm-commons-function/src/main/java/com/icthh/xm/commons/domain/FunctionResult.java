package com.icthh.xm.commons.domain;


public interface FunctionResult {

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
    String getRid();

    // todo: research
    Object functionResult();


}
