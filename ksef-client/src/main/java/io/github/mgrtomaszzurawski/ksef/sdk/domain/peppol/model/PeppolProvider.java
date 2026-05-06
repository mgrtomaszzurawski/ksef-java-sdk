/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import java.time.OffsetDateTime;

/**
 * A Peppol service provider registered in KSeF.
 *
 * @param id provider identifier
 * @param name provider display name
 * @param dateCreated registration timestamp in KSeF
 *
 * @since 1.0.0
 */
public record PeppolProvider(String id, String name, OffsetDateTime dateCreated) {

}
