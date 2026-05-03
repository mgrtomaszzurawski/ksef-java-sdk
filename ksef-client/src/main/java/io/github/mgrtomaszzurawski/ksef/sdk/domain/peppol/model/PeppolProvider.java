/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import java.time.OffsetDateTime;

/**
 * A Peppol service provider registered in KSeF.
 *
 * @param id provider identifier
 * @param name provider display name
 * @param dateCreated registration timestamp in KSeF
 */
public record PeppolProvider(String id, String name, OffsetDateTime dateCreated) {

}
