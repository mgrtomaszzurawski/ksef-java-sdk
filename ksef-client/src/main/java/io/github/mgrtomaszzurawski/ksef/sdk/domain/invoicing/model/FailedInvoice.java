/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One failed invoice in a {@code BatchResult}. Reports the per-invoice
 * reference number assigned by KSeF (when the server accepted the
 * envelope but rejected the content) along with the human-readable error
 * description and any structured detail strings.
 *
 * @param invoiceRef KSeF-assigned invoice reference number, or
 *     {@code null} when the failure occurred before the server could
 *     assign one (e.g. the batch was rejected wholesale)
 * @param error short human-readable error description
 * @param details additional detail messages from the server response;
 *     empty list when the server returned no extra details
 *
 * @since 1.0.0
 */
public record FailedInvoice(@Nullable String invoiceRef, String error, List<String> details) {

    private static final String ERR_ERROR_NULL = "error must not be null";
    private static final String ERR_DETAILS_NULL = "details must not be null";

    public FailedInvoice {
        Objects.requireNonNull(error, ERR_ERROR_NULL);
        Objects.requireNonNull(details, ERR_DETAILS_NULL);
        details = List.copyOf(details);
    }
}
