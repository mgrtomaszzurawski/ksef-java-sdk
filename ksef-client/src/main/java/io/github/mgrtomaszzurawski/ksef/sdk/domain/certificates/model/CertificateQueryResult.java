/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesResponseRaw;
import java.util.List;

/**
 * Result of querying certificates.
 *
 * @param certificates list of certificate items
 * @param hasMore whether more results are available
 */
public record CertificateQueryResult(List<CertificateListItem> certificates, boolean hasMore) {

    public static CertificateQueryResult from(QueryCertificatesResponseRaw raw) {
        List<CertificateListItem> mapped = raw.getCertificates() != null
                ? raw.getCertificates().stream().map(CertificateListItem::from).toList()
                : List.of();
        return new CertificateQueryResult(mapped, Boolean.TRUE.equals(raw.getHasMore()));
    }
}
