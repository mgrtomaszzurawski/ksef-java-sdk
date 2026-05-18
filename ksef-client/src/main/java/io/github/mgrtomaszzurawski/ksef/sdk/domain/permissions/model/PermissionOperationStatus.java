/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Terminal-state result of a synchronous permission operation
 * (every {@code grant*} / {@code revoke*} method on
 * {@code Permissions} after R1-11 + ADR-032).
 *
 * <p>The {@link #referenceNumber()} is the KSeF-assigned operation
 * identifier returned by the initial POST to the grant or revoke
 * endpoint; the SDK polls {@code /operations/{ref}} until terminal
 * internally, then surfaces both pieces of information on this record.
 * Consumers use {@code referenceNumber} as the {@code permissionId}
 * argument to {@code revokePermission} / {@code revokeAuthorization}
 * when they need to undo a freshly-issued grant without making an extra
 * {@code queryPersons} / {@code queryEntities} lookup.
 *
 * @param referenceNumber the KSeF operation reference (also used as the
 *     {@code permissionId} for the matching revoke); non-null
 * @param status terminal operation status (null only when the server
 *     omits the status block, which should not happen on terminal codes)
 *
 * @since 1.0.0
 */
public record PermissionOperationStatus(String referenceNumber, @Nullable StatusInfo status) {

    private static final String ERR_NULL_REFERENCE_NUMBER = "referenceNumber must not be null";

    public PermissionOperationStatus {
        Objects.requireNonNull(referenceNumber, ERR_NULL_REFERENCE_NUMBER);
    }
}
