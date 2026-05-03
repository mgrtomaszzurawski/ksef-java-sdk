/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.util.List;

/**
 * Result of retrieving certificates.
 *
 * @param certificates list of retrieved certificates
 */
public record RetrieveCertificatesResult(List<RetrievedCertificate> certificates) {

}
