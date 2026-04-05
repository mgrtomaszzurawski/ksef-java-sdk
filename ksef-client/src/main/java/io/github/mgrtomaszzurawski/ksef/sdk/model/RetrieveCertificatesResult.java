/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesResponseRaw;

import java.util.List;

/**
 * Result of retrieving certificates.
 *
 * @param certificates list of retrieved certificates
 */
public record RetrieveCertificatesResult(List<RetrievedCertificate> certificates) {

    public static RetrieveCertificatesResult from(RetrieveCertificatesResponseRaw raw) {
        List<RetrievedCertificate> mapped = raw.getCertificates() != null
                ? raw.getCertificates().stream().map(RetrievedCertificate::from).toList()
                : List.of();
        return new RetrieveCertificatesResult(mapped);
    }
}
