/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * VAT exemption basis on an invoice ({@code Fa/Adnotacje/Zwolnienie}).
 *
 * <p>An invoice is exempt from VAT when one of the three basis fields
 * is populated:
 * <ul>
 *   <li>{@link #legalBasisArticle()} — Polish-law article reference
 *       (e.g. {@code "art. 113 ust. 1 ustawy"} for the small-taxpayer
 *       exemption, {@code "art. 43 ust. 1 pkt 19"} for medical
 *       services).</li>
 *   <li>{@link #legalBasisDirective()} — EU VAT Directive article
 *       reference (e.g. {@code "art. 132"}).</li>
 *   <li>{@link #otherReason()} — free-text other basis (e.g.
 *       intra-Community supply with own EORI cert).</li>
 * </ul>
 *
 * <p>When no exemption applies the invoice carries
 * {@code Fa/Adnotacje/Zwolnienie/P_19N = TRUE} and the SDK exposes
 * {@code vatExemption()} as null.
 *
 * @param legalBasisArticle Polish-law article reference, or null
 * @param legalBasisDirective EU VAT Directive reference, or null
 * @param otherReason free-text other basis, or null
 *
 * @since 0.1.0
 */
public record VatExemption(
        @Nullable String legalBasisArticle,
        @Nullable String legalBasisDirective,
        @Nullable String otherReason) {
}
