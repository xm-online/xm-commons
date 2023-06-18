package com.icthh.xm.lep.api;

/**
 * The {@link LepInvocationCauseException} class.
 */
public class LepInvocationCauseException extends Exception {

    /**
     * Serial UID.
     */
    private static final long serialVersionUID = -6216272704551111089L;

    /**
     * This field holds the cause if the
     * LepInvocationCauseException(Throwable lepCause) constructor was
     * used to instantiate the object.
     */
    private Exception lepCause;

    /**
     * Constructs an {@code LepInvocationCauseException} with
     * {@code null} as the target exception.
     */
    private LepInvocationCauseException() {
        super((Exception) null);  // Disallow initCause
    }

    /**
     * Constructs a LepInvocationCauseException with a LEP exception.
     *
     * @param lepCause the LEP exception
     */
    public LepInvocationCauseException(Exception lepCause) {
        super((Exception) null);  // Disallow initCause
        this.lepCause = lepCause;
    }

    /**
     * Constructs a InvocationTargetException with a target exception
     * and a detail message.
     *
     * @param lepCause the LEP exception
     * @param msg      the detail message
     */
    public LepInvocationCauseException(Exception lepCause, String msg) {
        super(msg, null);  // Disallow initCause
        this.lepCause = lepCause;
    }

    /**
     * Returns the cause of this exception (the LEP thrown cause exception,
     * which may be {@code null}).
     *
     * @return the cause of this exception.
     */
    @Override
    public Exception getCause() {
        return lepCause;
    }

}

