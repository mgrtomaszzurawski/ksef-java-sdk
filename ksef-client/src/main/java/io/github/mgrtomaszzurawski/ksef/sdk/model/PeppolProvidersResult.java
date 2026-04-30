/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryPeppolProvidersResponseRaw;

import java.util.List;

/**
 * Page of Peppol service providers returned by a query.
 *
 * @param providers list of providers on the current page (never null, empty when no results)
 * @param hasMore true when additional pages of results are available
 */
public record PeppolProvidersResult(List<PeppolProvider> providers, boolean hasMore) {

    public static PeppolProvidersResult from(QueryPeppolProvidersResponseRaw raw) {
        List<PeppolProvider> mapped = raw.getPeppolProviders() != null
                ? raw.getPeppolProviders().stream().map(PeppolProvider::from).toList()
                : List.of();
        boolean more = raw.getHasMore() != null && raw.getHasMore();
        return new PeppolProvidersResult(mapped, more);
    }
}
