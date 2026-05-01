/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemRaw;
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

    public static CertificateListItem from(CertificateListItemRaw raw) {
        String subIdType = null;
        String subIdValue = null;
        if (raw.getSubjectIdentifier() != null) {
            subIdType = raw.getSubjectIdentifier().getType() != null
                    ? raw.getSubjectIdentifier().getType().getValue() : null;
            subIdValue = raw.getSubjectIdentifier().getValue();
        }
        return new CertificateListItem(
                raw.getCertificateSerialNumber(),
                raw.getName(),
                raw.getType() != null ? raw.getType().getValue() : null,
                raw.getCommonName(),
                raw.getStatus() != null ? raw.getStatus().getValue() : null,
                subIdType,
                subIdValue,
                raw.getValidFrom(),
                raw.getValidTo(),
                raw.getLastUseDate(),
                raw.getRequestDate());
    }
}
