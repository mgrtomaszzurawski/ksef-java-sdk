/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Sealed session lifecycle hierarchy: {@code Session} sealed interface
 * permits {@code OnlineSession} (writeable, AES key + IV in memory) and
 * {@code ClosedSession} (read-only post-close view for UPO + clearance).
 * Implementation classes live in this package per the ADR-024
 * reflective-bridge constraint (sealed impls share the permitting
 * interface's package).
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;
