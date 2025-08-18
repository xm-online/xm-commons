package com.icthh.xm.commons.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.domain.spec.IFunctionSpec;

/**
 * Interface for managing functions, allowing addition, updating, and removal of function specifications.
 * Implement this interface to provide functionality for managing function specifications.
 *
 * @param <FS>  the type of function specification
 * @param <FSE> the type of function specification with file name or other metadata
 */
public interface FunctionManageService<FS extends IFunctionSpec, FSE extends FunctionSpecWithFileName<FS>> {
    void addFunction(FSE newFunction);
    void updateFunction(FSE updatedFunction);
    void removeFunction(String functionKey);
    TypeReference<FSE> getFunctionSpecWrapperClass();
    TypeReference<FS> getFunctionSpecClass();
}
