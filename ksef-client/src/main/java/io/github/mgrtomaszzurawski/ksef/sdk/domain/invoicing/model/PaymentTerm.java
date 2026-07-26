/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * One payment term under {@code Fa/Platnosc/TerminPlatnosci}. A term
 * carries a concrete due date ({@code Termin}) and/or a descriptive
 * {@code TerminOpis}, both optional in the XSD.
 *
 * <p>{@code TerminOpis} is modelled differently by the two schemas, so the
 * fields are a union: FA(2) exposes it as free text (mapped to
 * {@link #description()}), while FA(3) structures it as a count of units
 * relative to a start event (mapped to {@link #quantity()} /
 * {@link #unit()} / {@link #startEvent()}). Only the source schema's subset
 * is populated; the rest are null.
 *
 * @param dueDate concrete due date ({@code Termin}) — null when absent
 * @param description free-text term description (FA(2) {@code TerminOpis}) — null on FA(3)
 * @param quantity number of units in a structured term (FA(3) {@code TerminOpis/Ilosc}) — null on FA(2)
 * @param unit unit of the structured term (FA(3) {@code TerminOpis/Jednostka}, e.g. "dni") — null on FA(2)
 * @param startEvent event the structured term is measured from (FA(3) {@code TerminOpis/ZdarzeniePoczatkowe}) — null on FA(2)
 *
 * @since 0.1.0
 */
public record PaymentTerm(
        @Nullable LocalDate dueDate,
        @Nullable String description,
        @Nullable Integer quantity,
        @Nullable String unit,
        @Nullable String startEvent) {
}
