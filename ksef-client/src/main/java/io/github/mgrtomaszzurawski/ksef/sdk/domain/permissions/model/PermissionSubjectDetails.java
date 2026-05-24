/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * Details about a permission subject (person or entity).
 *
 * <p>Covers both the person flavour ({@link #firstName()} +
 * {@link #surname()} + optional disambiguators like
 * {@link #personIdentifierValue()} / {@link #birthDate()} /
 * {@link #idDocumentNumber()}) and the entity flavour
 * ({@link #fullName()} + {@link #address()}). Use whichever subset is
 * non-null for the row — server populates only one flavour per
 * subject details row.
 *
 * @param firstName first name (persons only; null for entities)
 * @param surname surname (persons only; null for entities)
 * @param fullName full/trade name (entities only; null for persons)
 * @param personIdentifierType e.g. {@code "Pesel"}, {@code "Nip"}, or
 *     a foreign-identifier code (persons only)
 * @param personIdentifierValue raw identifier value matching
 *     {@link #personIdentifierType()} (persons only)
 * @param birthDate date of birth (persons only)
 * @param idDocumentType ID document type code (e.g. passport / ID card)
 *     (persons only)
 * @param idDocumentNumber ID document number (persons only)
 * @param idDocumentCountry ISO 3166-1 alpha-2 country code of the
 *     issuing authority (persons only)
 * @param address single-line address (entities only)
 *
 * @since 0.1.0
 */
public record PermissionSubjectDetails(
        @Nullable String firstName,
        @Nullable String surname,
        @Nullable String fullName,
        @Nullable String personIdentifierType,
        @Nullable String personIdentifierValue,
        @Nullable LocalDate birthDate,
        @Nullable String idDocumentType,
        @Nullable String idDocumentNumber,
        @Nullable String idDocumentCountry,
        @Nullable String address) {
}
