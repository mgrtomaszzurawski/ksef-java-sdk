/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
 */
public record FeaturePolicy(UpoVersion upoVersion, boolean problemDetails) {

    private static final String ERR_NULL_UPO = "upoVersion must not be null";

    public FeaturePolicy {
        Objects.requireNonNull(upoVersion, ERR_NULL_UPO);
    }

    /**
     * Default policy: server-default UPO version, RFC 7807 Problem Details
     * enabled. Matches pre-1.0 behavior.
     */
    public static FeaturePolicy defaults() {
        return new FeaturePolicy(UpoVersion.DEFAULT, true);
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

        public FeaturePolicy build() {
            return new FeaturePolicy(upoVersion, problemDetails);
        }
    }
}
