/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.InvoicingModeRaw;

/**
 * Mode of invoice submission.
 */
public enum InvoicingMode {

    ONLINE,
    OFFLINE;

    public static InvoicingMode from(InvoicingModeRaw raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw) {
            case ONLINE -> ONLINE;
            case OFFLINE -> OFFLINE;
        };
    }
}
