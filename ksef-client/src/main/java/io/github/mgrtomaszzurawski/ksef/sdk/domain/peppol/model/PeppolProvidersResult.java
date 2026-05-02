/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import java.util.List;

/**
 * Page of Peppol service providers returned by a query.
 *
 * @param providers list of providers on the current page (never null, empty when no results)
 * @param hasMore true when additional pages of results are available
 */
public record PeppolProvidersResult(List<PeppolProvider> providers, boolean hasMore) {

}
