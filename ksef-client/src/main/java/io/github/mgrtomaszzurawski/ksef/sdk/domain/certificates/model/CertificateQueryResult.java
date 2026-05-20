/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

import java.util.List;

/**
 * Single page of certificate list items returned by
 * {@code Certificates.queryCertificates(...)}.
 *
 * <p><strong>Paging flow.</strong> Inspect {@link #hasMore()} to decide
 * whether to fetch the next page; when {@code true}, build a new
 * {@link CertificateQueryRequest} with {@code pageOffset + 1} and the
 * same {@code pageSize}, then call {@code queryCertificates(...)}
 * again. When {@code false}, the current page is the last one. For
 * lazy full traversal use {@code Certificates.streamCertificates(...)}
 * instead — the SDK paginator handles offset arithmetic internally.
 *
 * <p>The KSeF spec does not surface a total count, so the only
 * navigation primitives are {@code hasMore} and the caller's running
 * {@code pageOffset}.
 *
 * @param certificates items on the current page (non-null, may be
 *     empty when the requested {@code pageOffset} lies past the result
 *     set)
 * @param hasMore {@code true} when at least one more page is available
 *     past the current {@code pageOffset}; {@code false} when this is
 *     the terminal page
 *
 * @since 0.1.0
 */
public record CertificateQueryResult(List<CertificateListItem> certificates, boolean hasMore) {

}
