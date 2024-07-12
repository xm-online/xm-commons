package com.icthh.xm.lep.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Represents the signature at a LEP method.
 * This interface parallels {@code java.lang.reflect.Member}.
 * {@code java.lang.reflect.Method} may don't have parameters name if code compiled without adding
 * this info in bytecode, so {@link MethodSignature} interface used.
 */
public interface MethodSignature {

    /**
     * Returns the identifier part of this signature.  For methods this
     * will return the method name.
     *
     * @return the identifier part of this signature
     * @see java.lang.reflect.Member#getName
     */
    String getName();

    /**
     * Returns the modifiers on this signature represented as an int.  Use
     * the constants and helper methods defined on {@code java.lang.reflect.Modifier} to manipulate this, i.e.
     * <pre>
     *     // check if this signature is public
     *     java.lang.reflect.Modifier.isPublic(sig.getModifiers());
     *
     *     // print out the modifiers
     *     java.lang.reflect.Modifier.toString(sig.getModifiers());
     * </pre>
     *
     * @return the modifiers on this signature represented as an int
     * @see java.lang.reflect.Member#getModifiers
     * @see java.lang.reflect.Modifier
     */
    int getModifiers();

    /**
     * <p>Returns a {@code java.lang.Class} object representing the class,
     * interface, or aspect (proxy) that declared this member.  For intra-member
     * declarations, this will be the type on which the member is declared,
     * not the type where the declaration is lexically written.  Use
     * {@code SourceLocation.getWithinType()} to get the type in
     * which the declaration occurs lexically.</p>
     * <p>For consistency with {@code java.lang.reflect.Member}, this
     * method named {@code getDeclaringClass()}.</p>
     *
     * @return a {@code java.lang.Class} object representing the class, interface,
     *     or aspect (proxy) that declared this member
     * @see java.lang.reflect.Member#getDeclaringClass
     */
    Class<?> getDeclaringClass();

    /**
     * Returns the fully-qualified name of the declaring type. This is
     * equivalent to calling getDeclaringClass().getName(), but caches
     * the result for greater efficiency.
     *
     * @return the fully-qualified name of the declaring type
     */
    String getDeclaringClassName();

    /**
     * Returns an array of {@code Class} objects that represent the formal
     * parameter types, in declaration order, of the method. Returns an array of length
     * 0 if the underlying method takes no parameters.
     *
     * @return the parameter types for the method
     */
    Class<?>[] getParameterTypes();

    /**
     * Returns an array of {@code String} objects that represent parameter names,
     * in declaration order, of the method. Returns an array of length
     * 0 if the underlying method takes no parameters.
     *
     * @return the parameter names for the method
     */
    String[] getParameterNames();

    /**
     * Returns list of {@code String} objects that represent parameter names,
     * in declaration order, of the method. Returns list of length
     * 0 if the underlying method takes no parameters.
     *
     * @return the parameter names for the method
     */
    List<String> getParameterNamesList();

    Integer getParameterIndex(String name);

    /**
     * Returns an array of {@code Class} objects that represent the
     * types of exceptions declared to be thrown by the underlying
     * method.  Returns an array of length 0 if the method declares no exceptions in its {@code
     * throws} clause.
     *
     * @return the exception types declared as being thrown by the method
     */
    Class<?>[] getExceptionTypes();

    /**
     * Returns a {@code Class} object that represents the formal return type
     * of the method.
     *
     * @return the return type for the method this object represents
     */
    Class<?> getReturnType();

    /**
     * Returns {@code Method} object reflecting the declared method.
     *
     * @return the {@code Method} object reflecting the declared method.
     */
    Method getMethod();

    String getLepContextMethodParameter();
}
