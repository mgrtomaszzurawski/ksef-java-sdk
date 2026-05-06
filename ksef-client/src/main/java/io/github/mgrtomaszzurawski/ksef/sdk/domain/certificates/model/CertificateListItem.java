/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.time.OffsetDateTime;

/**
 * Certificate summary in a query result.
 *
 * @param certificateSerialNumber serial number
 * @param name certificate name
 * @param type certificate type (Authentication or Offline)
 * @param commonName certificate CN
 * @param status lifecycle status (Active, Blocked, Revoked, Expired)
 * @param subjectIdentifierType subject identifier type (Nip, Pesel, Fingerprint)
 * @param subjectIdentifierValue subject identifier value
 * @param validFrom validity start
 * @param validTo validity end
 * @param lastUseDate last use timestamp
 * @param requestDate enrollment request date
 *
 * @since 1.0.0
 */
public record CertificateListItem(
        String certificateSerialNumber,
        String name,
        String type,
        String commonName,
        String status,
        String subjectIdentifierType,
        String subjectIdentifierValue,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        OffsetDateTime lastUseDate,
        OffsetDateTime requestDate) {

}
