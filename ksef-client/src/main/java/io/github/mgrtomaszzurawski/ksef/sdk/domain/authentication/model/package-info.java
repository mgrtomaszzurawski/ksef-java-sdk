/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Public records describing KSeF authentication-flow state — challenge
 * payloads, redeem responses, session listings, refresh-token info.
 *
 * <p>All records are immutable and constructed via {@code from(*Raw)}
 * factory methods that map the OpenAPI-generated transport types
 * (kept off the public surface per ADR-005).
 *
 * @since 1.0.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;
