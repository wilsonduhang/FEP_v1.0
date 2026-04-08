package com.puchain.fep.common.domain;

/**
 * Shared ENABLED/DISABLED status enum used across multiple config modules.
 *
 * <p>Replaces per-module duplicates: BusinessTypeStatus, OutputTypeStatus,
 * DataTypeConfigStatus, ReceiverStatus, InterfaceStatus.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum EnableDisableStatus {

    /** Enabled. */
    ENABLED,

    /** Disabled. */
    DISABLED
}
