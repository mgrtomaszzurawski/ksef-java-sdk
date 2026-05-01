/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;

/**
 * Data required for certificate enrollment (CSR generation).
 *
 * @param commonName CN field for the certificate
 * @param countryName country code
 * @param givenName first name (optional)
 * @param surname last name (optional)
 * @param serialNumber serial number (optional)
 * @param uniqueIdentifier unique identifier (optional)
 * @param organizationName organization name (optional)
 * @param organizationIdentifier organization identifier (optional)
 */
public record CertificateEnrollmentData(
        String commonName,
        String countryName,
        String givenName,
        String surname,
        String serialNumber,
        String uniqueIdentifier,
        String organizationName,
        String organizationIdentifier) {

    public static CertificateEnrollmentData from(CertificateEnrollmentDataResponseRaw raw) {
        return new CertificateEnrollmentData(
                raw.getCommonName(),
                raw.getCountryName(),
                raw.getGivenName(),
                raw.getSurname(),
                raw.getSerialNumber(),
                raw.getUniqueIdentifier(),
                raw.getOrganizationName(),
                raw.getOrganizationIdentifier());
    }
}
