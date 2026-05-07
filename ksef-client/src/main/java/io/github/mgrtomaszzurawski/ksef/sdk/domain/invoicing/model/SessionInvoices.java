/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * List of invoices within a session.
 *
 * @param continuationToken token for fetching next page, null if no more results
 * @param invoices invoice status items
 *
 * @since 1.0.0
 */
public record SessionInvoices(@Nullable String continuationToken, List<SessionInvoiceStatus> invoices) {

}
