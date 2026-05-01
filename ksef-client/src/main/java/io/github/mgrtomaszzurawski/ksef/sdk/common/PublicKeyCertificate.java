/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateRaw;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * KSeF public key certificate for encryption operations.
 *
 * @param certificate raw certificate bytes
 * @param validFrom certificate validity start
 * @param validTo certificate validity end
 * @param usage list of permitted usages for this certificate
 */
public record PublicKeyCertificate(
        byte[] certificate,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        List<PublicKeyCertificateUsage> usage) {

    public static PublicKeyCertificate from(PublicKeyCertificateRaw raw) {
        List<PublicKeyCertificateUsage> mappedUsage = raw.getUsage() != null
                ? raw.getUsage().stream().map(PublicKeyCertificateUsage::from).toList()
                : List.of();
        return new PublicKeyCertificate(
                raw.getCertificate(),
                raw.getValidFrom(),
                raw.getValidTo(),
                mappedUsage);
    }
}
