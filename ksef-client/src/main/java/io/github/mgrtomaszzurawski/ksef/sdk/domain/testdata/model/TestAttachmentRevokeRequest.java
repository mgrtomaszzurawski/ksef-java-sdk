/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code TestDataAdmin.revokeAttachment(...)}. Carries
 * the NIP-typed subject whose attachment permission is being revoked
 * plus an optional date when the revocation takes effect.
 *
 * <p>Maps to the wire's {@code AttachmentPermissionRevokeRequest}
 * (POST {@code /testdata/attachment/revoke}): {@code nip} is required,
 * {@code expectedEndDate} is nullable per OpenAPI. When the field is
 * omitted ({@link #expectedEndDate()} is {@code null}), KSeF applies
 * server-side default revocation behaviour per spec wire description
 * "Data wycofania zgody na przesyłanie faktur z załącznikiem".
 *
 * @param subject the NIP-typed subject losing attachment permission
 *     (non-null; {@code KsefIdentifier.Type.NIP} required, runtime
 *     check in impl)
 * @param expectedEndDate optional date when the revocation takes
 *     effect; {@code null} = field omitted on wire (server defaults)
 *
 * @since 1.0.0
 */
public record TestAttachmentRevokeRequest(
        KsefIdentifier subject,
        @Nullable LocalDate expectedEndDate) {

    public TestAttachmentRevokeRequest {
        Objects.requireNonNull(subject, "subject");
    }

    /** Build a request with no end date (server-default revocation behaviour). */
    public static TestAttachmentRevokeRequest immediate(KsefIdentifier subject) {
        return new TestAttachmentRevokeRequest(subject, null);
    }

    /** Build a request scheduling revocation for a specific date. */
    public static TestAttachmentRevokeRequest scheduled(KsefIdentifier subject, LocalDate expectedEndDate) {
        return new TestAttachmentRevokeRequest(subject,
                Objects.requireNonNull(expectedEndDate, "expectedEndDate"));
    }
}
