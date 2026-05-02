/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceStatusInfoRaw;
import java.util.List;
import java.util.Map;

/**
 * Status information for an invoice, with optional extensions.
 *
 * @param code numeric status code
 * @param description human-readable status description
 * @param details optional additional detail messages
 * @param extensions optional key-value extensions
 */
public record InvoiceStatusInfo(int code, String description, List<String> details, Map<String, String> extensions) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static InvoiceStatusInfo from(InvoiceStatusInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new InvoiceStatusInfo(
                raw.getCode(),
                raw.getDescription(),
                raw.getDetails() != null ? List.copyOf(raw.getDetails()) : List.of(),
                raw.getExtensions() != null ? Map.copyOf(raw.getExtensions()) : Map.of());
    }
}
