package com.icthh.xm.commons.messaging.event.system;

/**
 * The {@link SystemEventType} class.
 */
public final class SystemEventType {

    /**
     * Event source: any app/ms.
     * Description: inform about some ms/app current scanned privileges.
     */
    public static final String MS_PRIVILEGES = "MS_PRIVILEGES";

    /**
     * Event source: MS-ENTITY.
     * Description: inform about Profile changes while save related XmEntity with type key 'ACCOUNT.USER'.
     */
    public static final String UPDATE_ACCOUNT = "UPDATE_ACCOUNT";

    /**
     * Event source: UAA.
     * Description: inform about UAA user (account) is activated.
     */
    public static final String ACTIVATE_PROFILE = "ACTIVATE_PROFILE";

    /**
     * Event source: UAA.
     * Description: inform about that uaa account registered (on privilege: ACCOUNT.REGISTER)
     * or that user created (privilege: USER.CREATE).
     */
    public static final String CREATE_PROFILE = "CREATE_PROFILE";

    /**
     * Event source: UAA.
     * Description: inform about that uaa account password changed (on privilege: ACCOUNT.PASSWORD.UPDATE).
     */
    public static final String CHANGE_PASSWORD = "CHANGE_PASSWORD";

    /**
     * Event source: UAA.
     * Description: inform about that uaa account (or user) profile data changed (on privileges: ACCOUNT.UPDATE,
     * ACCOUNT.LOGIN.UPDATE, USER.UPDATE, USER.LOGIN.UPDATE).
     */
    public static final String UPDATE_PROFILE = "UPDATE_PROFILE";

    /**
     * Event source: UAA.
     * Description:  This event inform about new created tenant.
     * Now this event produced and consumed by UAA and now used for xm-ms-timeline clusterization cases only.
     */
    // TODO refactor event name and constant name
    public static final String CREATE_COMMAND = "CREATE";

    /**
     * Event source: UAA.
     * Description: This event inform about deleted tenant.
     * Now this event produced and consumed by UAA and now used for xm-ms-timeline clusterization cases only.
     */
    // TODO refactor event name and constant name
    public static final String DELETE_COMMAND = "DELETE";

    private SystemEventType() {
        throw new UnsupportedOperationException();
    }

}
