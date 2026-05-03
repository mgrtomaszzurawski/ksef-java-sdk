/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
