/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Read-side workflow types: {@code InvoiceArchive} (by-KSeF-number + by-metadata
 * fetch), {@code InvoiceBatch} (bulk submission), {@code InvoiceExport} +
 * {@code PreparedInvoiceExport} (encrypted package download), and
 * {@code InvoiceSync} (incremental sync orchestrator).
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.archive;
