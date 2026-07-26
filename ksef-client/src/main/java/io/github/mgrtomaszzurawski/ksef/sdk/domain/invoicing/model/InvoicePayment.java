/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Payment terms of an FA(2)/FA(3) invoice — the typed view of the
 * {@code Fa/Platnosc} node. Null on {@code InvoiceDocument.payment()}
 * when the invoice carries no payment block.
 *
 * <p>The payment form is an XSD choice: either a coded
 * {@link #methodCode()} ({@code FormaPlatnosci}) or the "other" form
 * ({@link #otherForm()} = {@code PlatnoscInna}) detailed by
 * {@link #otherDescription()}. The list-valued members are always
 * non-null (empty when absent).
 *
 * @param paid paid-in-full marker ({@code Zaplacono = 1}); null when not flagged
 * @param paymentDate date the invoice was paid ({@code DataZaplaty}); null when unpaid at issue
 * @param partialPaymentStatus partial-payment marker ({@code ZnacznikZaplatyCzesciowej}:
 *     1 = paid in part, 2 = paid in full across two or more installments); null when not flagged
 * @param methodCode coded payment form ({@code FormaPlatnosci}, e.g. "1" cash, "2" card, "6" transfer); null when "other"
 * @param otherForm "other payment form" marker ({@code PlatnoscInna = 1}); null when a coded form is used
 * @param otherDescription free-text detail of the other form ({@code OpisPlatnosci}); null when a coded form is used
 * @param paymentLink cashless payment link ({@code LinkDoPlatnosci}); null when not supplied
 * @param ipKsef KSeF payment identifier ({@code IPKSeF}); null when not supplied
 * @param terms payment terms ({@code TerminPlatnosci}); empty when none
 * @param partialPayments installments already received ({@code ZaplataCzesciowa}); empty when none
 * @param bankAccounts seller settlement accounts ({@code RachunekBankowy}); empty when none
 * @param factorBankAccounts factor accounts ({@code RachunekBankowyFaktora}); empty when none
 * @param earlyPaymentDiscount early-payment discount ({@code Skonto}); null when none
 *
 * @since 0.1.0
 */
public record InvoicePayment(
        @Nullable Boolean paid,
        @Nullable LocalDate paymentDate,
        @Nullable Integer partialPaymentStatus,
        @Nullable String methodCode,
        @Nullable Boolean otherForm,
        @Nullable String otherDescription,
        @Nullable String paymentLink,
        @Nullable String ipKsef,
        List<PaymentTerm> terms,
        List<PartialPayment> partialPayments,
        List<BankAccount> bankAccounts,
        List<BankAccount> factorBankAccounts,
        @Nullable EarlyPaymentDiscount earlyPaymentDiscount) {

    public InvoicePayment {
        terms = List.copyOf(terms);
        partialPayments = List.copyOf(partialPayments);
        bankAccounts = List.copyOf(bankAccounts);
        factorBankAccounts = List.copyOf(factorBankAccounts);
    }
}
