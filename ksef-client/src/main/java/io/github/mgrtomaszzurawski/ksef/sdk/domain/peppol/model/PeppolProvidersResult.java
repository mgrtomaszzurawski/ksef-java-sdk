/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import java.util.List;

/**
 * Single page of Peppol service providers returned by
 * {@code PeppolProviders.queryProviders(...)}.
 *
 * <p><strong>Paging flow.</strong> Inspect {@link #hasMore()} to decide
 * whether to fetch the next page; when {@code true}, build a new
 * {@link PeppolProvidersQueryRequest} with {@code pageOffset + 1} and
 * the same {@code pageSize}, then call
 * {@code queryProviders(...)} again. When {@code false}, the current
 * page is the last one. For lazy full traversal use
 * {@code PeppolProviders.streamProviders()} instead — the SDK
 * paginator handles offset arithmetic internally.
 *
 * <p>The KSeF spec does not surface a total count, so the only
 * navigation primitives are {@code hasMore} and the caller's running
 * {@code pageOffset}.
 *
 * @param providers items on the current page (non-null, may be empty
 *     when the requested {@code pageOffset} lies past the result set)
 * @param hasMore {@code true} when at least one more page is available
 *     past the current {@code pageOffset}; {@code false} when this is
 *     the terminal page
 *
 * @since 1.0.0
 */
public record PeppolProvidersResult(List<PeppolProvider> providers, boolean hasMore) {

}
