package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepResourceType;

/**
 * XM LEP resource sub type.
 */
public enum XmLepResourceSubType implements LepResourceType {

    /**
     * Before: type for resource that executes before a LEP method, but which does not have the ability to prevent
     * execution flow proceeding to the LEP (unless it throws an exception).
     */
    BEFORE {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "before";
        }
    },

    /**
     * Around: type for resource that surrounds a LEP such as a method invocation. This is the most powerful kind
     * of LEP resource type. Around resource can perform custom behavior before and after the method invocation.
     */
    AROUND {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "around";
        }
    },

    /**
     * Tenant: replace {@link #DEFAULT} or use new resource with tenant specific LEP implementation.
     */
    TENANT {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "tenant";
        }
    },

    /**
     * Default: resource with all tenants common LEP implementation.
     */
    DEFAULT {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "default";
        }
    },

    /**
     * After (finally): type for resource to be executed regardless of the means by which a LEP exits (normal or
     * exceptional return).
     */
    AFTER {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "after";
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

}
