/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;

/**
 * List of invoices within a session.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param invoices invoice status items
 */
public record SessionInvoices(String continuationToken, List<SessionInvoiceStatus> invoices) {

}
