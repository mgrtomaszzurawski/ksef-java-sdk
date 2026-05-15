/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bundle of KSeF authorisation + visualisation context that an offline
 * invoice needs to render its KOD I + KOD II QR codes correctly. Passed
 * to {@link OfflineSigningProvider#signAndPackage} and
 * {@code OfflineInvoice.fromInvoice} so call sites read "invoice +
 * signing certificate + context" rather than carrying five loose
 * arguments through the chain.
 *
 * @param environment QR environment whose host appears inside the
 *     KOD I and KOD II verification URLs
 * @param contextType authorising context kind embedded in KOD II URL
 *     (typically {@link QrContextType#NIP})
 * @param contextValue value of the authorising context (typically the
 *     seller NIP)
 * @param sellerNip 10-digit seller NIP embedded in the KOD I URL
 * @param issueDate calendar date embedded in the KOD I URL (the
 *     invoice's issue date in KSeF wire-time semantics)
 *
 * @since 1.0.0
 */
public record OfflineSigningContext(
        QrEnvironment environment,
        QrContextType contextType,
        String contextValue,
        String sellerNip,
        LocalDate issueDate) {

    private static final String ERR_NULL_ENV = "environment must not be null";
    private static final String ERR_NULL_CONTEXT_TYPE = "contextType must not be null";
    private static final String ERR_NULL_CONTEXT_VALUE = "contextValue must not be null";
    private static final String ERR_NULL_SELLER_NIP = "sellerNip must not be null";
    private static final String ERR_NULL_ISSUE_DATE = "issueDate must not be null";

    public OfflineSigningContext {
        Objects.requireNonNull(environment, ERR_NULL_ENV);
        Objects.requireNonNull(contextType, ERR_NULL_CONTEXT_TYPE);
        Objects.requireNonNull(contextValue, ERR_NULL_CONTEXT_VALUE);
        Objects.requireNonNull(sellerNip, ERR_NULL_SELLER_NIP);
        Objects.requireNonNull(issueDate, ERR_NULL_ISSUE_DATE);
    }
}
