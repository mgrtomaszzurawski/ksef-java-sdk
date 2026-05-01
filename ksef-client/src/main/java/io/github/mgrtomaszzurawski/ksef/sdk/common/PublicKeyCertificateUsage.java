/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateUsageRaw;

/**
 * Usage type of a KSeF public key certificate.
 */
public enum PublicKeyCertificateUsage {

    KSEF_TOKEN_ENCRYPTION,
    SYMMETRIC_KEY_ENCRYPTION;

    public static PublicKeyCertificateUsage from(PublicKeyCertificateUsageRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case KSEF_TOKEN_ENCRYPTION -> KSEF_TOKEN_ENCRYPTION;
            case SYMMETRIC_KEY_ENCRYPTION -> SYMMETRIC_KEY_ENCRYPTION;
        };
    }
}
