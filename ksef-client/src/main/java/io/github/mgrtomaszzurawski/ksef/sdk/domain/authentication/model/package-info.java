/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Public records describing KSeF authentication-flow state — challenge
 * payloads, redeem responses, session listings, refresh-token info.
 *
 * <p>All records are immutable and returned by SDK clients/builders.
 * The mapping from OpenAPI-generated transport types is handled
 * internally and does not appear on the public surface (per ADR-005).
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;
