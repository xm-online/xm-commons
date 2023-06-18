package com.icthh.xm.lep.api;

/**
 * The {@link LepKey} interface is the top-level interface for all keys.
 * This key uniquely identifies LEP.
 */
@Deprecated(forRemoval = true)
public interface LepKey {

    /**
     * Returns LEP identificator.
     *
     * @return LEP identificator
     */
    String getId();

    /**
     * Returns group key to which belongs this LepKey.
     *
     * <p>For top level group or if group not specified for this LepKey instance
     * the method can return {@code null}.
     *
     * @return group key or {@code null}
     */
    LepKey getGroupKey();

}
