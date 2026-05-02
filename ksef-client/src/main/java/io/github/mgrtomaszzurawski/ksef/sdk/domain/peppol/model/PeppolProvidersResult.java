/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

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
        List<PeppolProvider> mapped = raw.getPeppolProviders().stream().map(PeppolProvider::from).toList();
        return new PeppolProvidersResult(mapped, raw.getHasMore());
    }
}
