/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CheckAttachmentPermissionStatusResponseRaw;
import java.time.OffsetDateTime;

/**
 * Status of attachment permission for the current context.
 *
 * @param attachmentAllowed whether attachments are allowed
 * @param revokedDate when the permission was revoked (null if active)
 */
public record AttachmentPermissionStatus(Boolean attachmentAllowed, OffsetDateTime revokedDate) {

    public static AttachmentPermissionStatus from(CheckAttachmentPermissionStatusResponseRaw raw) {
        return new AttachmentPermissionStatus(
                raw.getIsAttachmentAllowed(),
                raw.getRevokedDate());
    }
}
