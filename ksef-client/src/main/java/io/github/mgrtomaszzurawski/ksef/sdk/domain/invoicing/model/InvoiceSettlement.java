/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Additional settlements on an FA(2)/FA(3) invoice — the typed view of
 * the {@code Fa/Rozliczenie} node. Null on {@code InvoiceDocument.settlement()}
 * when the invoice carries no settlement block.
 *
 * <p>{@code amountDue} and {@code amountToSettle} are an XSD choice: an
 * invoice states either the amount payable after charges/deductions
 * ({@code DoZaplaty}) or an overpayment to be settled ({@code DoRozliczenia}),
 * so at most one is non-null. The list-valued members are always non-null
 * (empty when absent).
 *
 * @param charges amounts added to {@code P_15} ({@code Obciazenia}); empty when none
 * @param chargesTotal sum of charges ({@code SumaObciazen}); null when not supplied
 * @param deductions amounts subtracted from {@code P_15} ({@code Odliczenia}); empty when none
 * @param deductionsTotal sum of deductions ({@code SumaOdliczen}); null when not supplied
 * @param amountDue amount payable ({@code DoZaplaty} = {@code P_15} + charges − deductions); null when an overpayment is stated instead
 * @param amountToSettle overpayment to refund / carry forward ({@code DoRozliczenia}); null when an amount is payable instead
 *
 * @since 0.1.0
 */
public record InvoiceSettlement(
        List<SettlementItem> charges,
        @Nullable BigDecimal chargesTotal,
        List<SettlementItem> deductions,
        @Nullable BigDecimal deductionsTotal,
        @Nullable BigDecimal amountDue,
        @Nullable BigDecimal amountToSettle) {

    public InvoiceSettlement {
        charges = List.copyOf(charges);
        deductions = List.copyOf(deductions);
    }
}
