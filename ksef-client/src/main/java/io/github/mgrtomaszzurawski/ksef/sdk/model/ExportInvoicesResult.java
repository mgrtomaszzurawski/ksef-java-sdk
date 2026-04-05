/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.ExportInvoicesResponseRaw;

/**
 * Result of starting an invoice export job.
 *
 * @param referenceNumber export reference number for status polling
 */
public record ExportInvoicesResult(String referenceNumber) {

    public static ExportInvoicesResult from(ExportInvoicesResponseRaw raw) {
        return new ExportInvoicesResult(raw.getReferenceNumber());
    }
}
