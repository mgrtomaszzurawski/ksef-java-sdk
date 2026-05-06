/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

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
 *
 * @since 1.0.0
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

}
