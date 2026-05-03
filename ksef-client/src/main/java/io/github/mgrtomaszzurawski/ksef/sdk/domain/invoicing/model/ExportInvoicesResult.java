/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Result of starting an invoice export job.
 *
 * @param referenceNumber export reference number for status polling
 */
public record ExportInvoicesResult(String referenceNumber) {

}
