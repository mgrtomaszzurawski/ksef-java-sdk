/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;

/**
 * Result of opening an online (interactive) session.
 *
 * @param referenceNumber session reference number for subsequent operations
 * @param validUntil session expiration timestamp
 */
public record OnlineSession(String referenceNumber, OffsetDateTime validUntil) {

}
