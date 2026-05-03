/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicKeyCertificate other)) {
            return false;
        }
        return Arrays.equals(certificate, other.certificate)
                && Objects.equals(validFrom, other.validFrom)
                && Objects.equals(validTo, other.validTo)
                && Objects.equals(usage, other.usage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validFrom, validTo, usage, Arrays.hashCode(certificate));
    }

    @Override
    public String toString() {
        return "PublicKeyCertificate[certificate=byte[" + (certificate == null ? 0 : certificate.length) + "]"
                + ", validFrom=" + validFrom
                + ", validTo=" + validTo
                + ", usage=" + usage + "]";
    }
}
