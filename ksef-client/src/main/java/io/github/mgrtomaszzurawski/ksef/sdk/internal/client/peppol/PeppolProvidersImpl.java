/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol;

import io.github.mgrtomaszzurawski.ksef.client.model.QueryPeppolProvidersResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.PeppolProviders;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol.mapping.PeppolMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for KSeF Peppol service provider queries.
 *
 * <p>Lists Peppol service providers registered in KSeF. Results are sorted by
 * {@code dateCreated} descending, then {@code id} ascending. Requires authentication.
 *
 * @since 1.0.0
 */
public final class PeppolProvidersImpl implements PeppolProviders {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolProvidersImpl.class);
    private static final String LOG_CALL = "→ {} pageOffset={} pageSize={}";

    private static final String PATH_PEPPOL_QUERY = ApiPaths.PEPPOL + "/query";
    private static final String OP_QUERY_PROVIDERS = "queryPeppolProviders";

    private static final String QUERY_PARAM_PAGE_OFFSET = "pageOffset";
    private static final String QUERY_PARAM_PAGE_SIZE = "pageSize";

    private static final String QUERY_STRING_PREFIX = "?";
    private static final String QUERY_PARAM_ASSIGN = "=";
    private static final String QUERY_PARAM_SEPARATOR = "&";

    private static final String ERR_PAGE_OFFSET_NEGATIVE = "pageOffset must be >= 0";
    private static final String ERR_PAGE_SIZE_NOT_POSITIVE = "pageSize must be > 0";

    /**
     * Page size used by {@link #streamProviders()} on each underlying
     * page fetch. Picked to balance request count (smaller = more
     * round-trips) against per-page memory cost (larger = bigger
     * response payload). Spec does not document a server-side maximum
     * for this endpoint; 50 matches the demo runner's manual paging.
     */
    private static final int STREAM_PAGE_SIZE = 50;

    private final HttpSupport http;

    public PeppolProvidersImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Query a specific page of Peppol service providers.
     *
     * @param pageOffset zero-based page index (must be {@code >= 0})
     * @param pageSize number of items per page (must be {@code > 0})
     * @return the requested page of providers
     */
    @Override
    public PeppolProvidersResult query(int pageOffset, int pageSize) {
        LOGGER.debug(LOG_CALL, OP_QUERY_PROVIDERS, pageOffset, pageSize);
        if (pageOffset < 0) {
            throw new IllegalArgumentException(ERR_PAGE_OFFSET_NEGATIVE);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException(ERR_PAGE_SIZE_NOT_POSITIVE);
        }
        return fetchPage(pageOffset, pageSize);
    }

    @Override
    public Stream<PeppolProvider> streamProviders() {
        return PagedSpliterator.stream(pageOffset -> {
            PeppolProvidersResult page = fetchPage(pageOffset, STREAM_PAGE_SIZE);
            return new PagedSpliterator.Page<>(page.providers(), page.hasMore());
        });
    }

    private PeppolProvidersResult fetchPage(int pageOffset, int pageSize) {
        String path = PATH_PEPPOL_QUERY
                + QUERY_STRING_PREFIX + QUERY_PARAM_PAGE_OFFSET + QUERY_PARAM_ASSIGN + pageOffset
                + QUERY_PARAM_SEPARATOR + QUERY_PARAM_PAGE_SIZE + QUERY_PARAM_ASSIGN + pageSize;
        String token = http.requireToken();
        QueryPeppolProvidersResponseRaw rawValue = http.getAuthenticated(path, token,
                QueryPeppolProvidersResponseRaw.class, OP_QUERY_PROVIDERS);
        return PeppolMappers.toPeppolProvidersResult(rawValue);
    }
}
