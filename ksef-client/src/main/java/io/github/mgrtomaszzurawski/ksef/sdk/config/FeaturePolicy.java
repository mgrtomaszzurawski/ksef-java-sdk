/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.util.Objects;

/**
 * Centralised KSeF feature-flag configuration for a {@code KsefClient}.
 *
 * <p>Holds the consumer-facing settings that map to
 * {@code X-KSeF-Feature} and {@code X-Error-Format} HTTP headers. The
 * mapping from these typed options to wire header strings stays internal.
 *
 * <p>Build via {@link #builder()} or use {@link #defaults()}.
 *
 * @param upoVersion which UPO schema version to request (default
 *     {@link UpoVersion#DEFAULT} — no header)
 * @param problemDetails whether to request RFC 7807 Problem Details
 *     bodies on 4xx/5xx responses (default {@code true})
 * @param enforceXadesCompliance whether to send the early-adopt
 *     {@code X-KSeF-Feature: enforce-xades-compliance} header on the
 *     XAdES authentication paths ({@code /auth/xades-signature} and
 *     {@code /auth/ksef-token}). Surfaces strict XAdES validation on
 *     DEMO/PROD before the global cutover. Default {@code false} —
 *     keep production callers on lenient validation until they are
 *     ready (api-changelog v2.1.1).
 *
 * @since 1.0.0
 */
public record FeaturePolicy(UpoVersion upoVersion, boolean problemDetails, boolean enforceXadesCompliance) {

    private static final String ERR_NULL_UPO = "upoVersion must not be null";

    public FeaturePolicy {
        Objects.requireNonNull(upoVersion, ERR_NULL_UPO);
    }

    /**
     * Default policy: server-default UPO version, RFC 7807 Problem Details
     * enabled, lenient XAdES validation.
     */
    public static FeaturePolicy defaults() {
        return new FeaturePolicy(UpoVersion.DEFAULT, true, false);
    }

    /**
     * Mutable builder. Use when assembling a policy from configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private UpoVersion upoVersion = UpoVersion.DEFAULT;
        private boolean problemDetails = true;
        private boolean enforceXadesCompliance;

        private Builder() { }

        /**
         * Select the UPO schema version. Default: {@link UpoVersion#DEFAULT}.
         */
        public Builder upoVersion(UpoVersion value) {
            this.upoVersion = Objects.requireNonNull(value, ERR_NULL_UPO);
            return this;
        }

        /**
         * Toggle RFC 7807 Problem Details opt-in. Default: {@code true}.
         */
        public Builder problemDetails(boolean enabled) {
            this.problemDetails = enabled;
            return this;
        }

        /**
         * Toggle {@code X-KSeF-Feature: enforce-xades-compliance} for the
         * XAdES auth paths. Default: {@code false}. Enable to opt in to
         * the strict XAdES validator on DEMO/PROD before the global
         * cutover (api-changelog v2.1.1).
         */
        public Builder enforceXadesCompliance(boolean enabled) {
            this.enforceXadesCompliance = enabled;
            return this;
        }

        public FeaturePolicy build() {
            return new FeaturePolicy(upoVersion, problemDetails, enforceXadesCompliance);
        }
    }
}
