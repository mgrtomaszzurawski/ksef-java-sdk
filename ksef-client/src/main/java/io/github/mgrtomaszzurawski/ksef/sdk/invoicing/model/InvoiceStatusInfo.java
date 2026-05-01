/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

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

    public static InvoiceStatusInfo from(InvoiceStatusInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new InvoiceStatusInfo(
                raw.getCode() != null ? raw.getCode() : 0,
                raw.getDescription(),
                raw.getDetails() != null ? List.copyOf(raw.getDetails()) : List.of(),
                raw.getExtensions() != null ? Map.copyOf(raw.getExtensions()) : Map.of());
    }
}
