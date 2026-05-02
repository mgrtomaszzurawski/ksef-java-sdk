/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import io.github.mgrtomaszzurawski.ksef.client.model.PeppolProviderRaw;
import java.time.OffsetDateTime;

/**
 * A Peppol service provider registered in KSeF.
 *
 * @param id provider identifier
 * @param name provider display name
 * @param dateCreated registration timestamp in KSeF
 */
public record PeppolProvider(String id, String name, OffsetDateTime dateCreated) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static PeppolProvider from(PeppolProviderRaw raw) {
        return new PeppolProvider(raw.getId(), raw.getName(), raw.getDateCreated());
    }
}
