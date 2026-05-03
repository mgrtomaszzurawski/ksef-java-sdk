/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.util.Objects;

/**
 * XAdES signing options for certificate-based authentication credentials.
 *
 * <p>Currently the SDK supports {@link XadesProfile#BASELINE_B} with
 * {@link DigestAlgorithm#SHA256} only. The
 * {@code ksef-docs/auth/podpis-xades.md} spec lists more profiles (T,
 * LT, LTA, ...) and digests (SHA-1, SHA-384, SHA-512, SHA3-*) but those
 * are not implemented in 1.0.0. Additional enum members will be added
 * only when matching test coverage is shipped per ADR-021 (public API
 * must mean working support, not aspirational support).
 *
 * @param xadesProfile XAdES profile (currently only {@link XadesProfile#BASELINE_B})
 * @param digestAlgorithm digest algorithm (currently only {@link DigestAlgorithm#SHA256})
 */
public record SigningOptions(XadesProfile xadesProfile, DigestAlgorithm digestAlgorithm) {

    private static final String ERR_NULL_PROFILE = "xadesProfile must not be null";
    private static final String ERR_NULL_DIGEST = "digestAlgorithm must not be null";

    public SigningOptions {
        Objects.requireNonNull(xadesProfile, ERR_NULL_PROFILE);
        Objects.requireNonNull(digestAlgorithm, ERR_NULL_DIGEST);
    }

    /**
     * Default options: XAdES-BASELINE-B with SHA-256. Matches pre-1.0
     * behavior and current SDK support scope.
     */
    public static SigningOptions defaults() {
        return new SigningOptions(XadesProfile.BASELINE_B, DigestAlgorithm.SHA256);
    }

    /**
     * XAdES profiles supported by this SDK. The KSeF spec at
     * {@code auth/podpis-xades.md:24-38} permits BASELINE-T/LT/LTA/A/ERS
     * etc. but those are not currently exposed; see ADR-021.
     */
    public enum XadesProfile {
        /** XAdES-BASELINE-B — minimum profile, no timestamp / no LT validation data. */
        BASELINE_B
    }

    /**
     * Digest algorithms supported by this SDK. The KSeF spec at
     * {@code auth/podpis-xades.md:73-87} permits SHA-1/384/512/SHA3-*
     * but those are not currently exposed; see ADR-021.
     */
    public enum DigestAlgorithm {
        /** SHA-256. */
        SHA256
    }
}
