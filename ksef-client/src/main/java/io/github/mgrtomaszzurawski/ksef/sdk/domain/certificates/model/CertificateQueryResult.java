/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.util.List;

/**
 * Result of querying certificates.
 *
 * @param certificates list of certificate items
 * @param hasMore whether more results are available
 */
public record CertificateQueryResult(List<CertificateListItem> certificates, boolean hasMore) {

}
