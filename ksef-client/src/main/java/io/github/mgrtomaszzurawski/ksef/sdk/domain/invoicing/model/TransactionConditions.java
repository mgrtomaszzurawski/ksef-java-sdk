/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The commercial and logistic conditions of a transaction — the typed
 * view of the {@code Fa/WarunkiTransakcji} node. Null on
 * {@code InvoiceDocument.transactionConditions()} when the invoice
 * carries no such block. Every member is optional in the XSD.
 *
 * <p>{@link #contractExchangeRate()} and {@link #contractCurrency()} form
 * an XSD pair: both are present or both absent, describing the rate at
 * which amounts shown on the invoice in złoty were converted.
 *
 * @param agreements underlying contracts ({@code Umowy}); empty when none
 * @param orders underlying purchase orders ({@code Zamowienia}); empty when none
 * @param productBatchNumbers product batch numbers ({@code NrPartiiTowaru}); empty when none
 * @param deliveryTerms delivery terms, e.g. Incoterms ({@code WarunkiDostawy}); null when not supplied
 * @param contractExchangeRate contractual exchange rate ({@code KursUmowny}); null when not supplied
 * @param contractCurrency contractual currency, ISO 4217 ({@code WalutaUmowna}); null when not supplied
 * @param transports transport legs ({@code Transport}); empty when none
 * @param intermediary intermediary-supply marker ({@code PodmiotPosredniczacy = 1}, art. 22(2d)); null when not flagged
 *
 * @since 0.1.0
 */
public record TransactionConditions(
        List<Agreement> agreements,
        List<PurchaseOrder> orders,
        List<String> productBatchNumbers,
        @Nullable String deliveryTerms,
        @Nullable BigDecimal contractExchangeRate,
        @Nullable String contractCurrency,
        List<Transport> transports,
        @Nullable Boolean intermediary) {

    public TransactionConditions {
        agreements = List.copyOf(agreements);
        orders = List.copyOf(orders);
        productBatchNumbers = List.copyOf(productBatchNumbers);
        transports = List.copyOf(transports);
    }
}
