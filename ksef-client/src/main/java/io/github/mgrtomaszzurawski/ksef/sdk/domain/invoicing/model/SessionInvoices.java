/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.client.model.SessionInvoicesResponseRaw;
import java.util.List;

/**
 * List of invoices within a session.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param invoices invoice status items
 */
public record SessionInvoices(String continuationToken, List<SessionInvoiceStatus> invoices) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static SessionInvoices from(SessionInvoicesResponseRaw raw) {
        List<SessionInvoiceStatus> mapped = raw.getInvoices().stream().map(SessionInvoiceStatus::from).toList();
        return new SessionInvoices(raw.getContinuationToken(), mapped);
    }
}
