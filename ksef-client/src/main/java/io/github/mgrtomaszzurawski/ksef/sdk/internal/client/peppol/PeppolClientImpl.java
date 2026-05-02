/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.peppol;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.PeppolClient;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryPeppolProvidersResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;

/**
 * Client for KSeF Peppol service provider queries.
 *
 * <p>Lists Peppol service providers registered in KSeF. Results are sorted by
 * {@code dateCreated} descending, then {@code id} ascending. Requires authentication.
 */
public final class PeppolClientImpl implements PeppolClient {

    private static final String PATH_PEPPOL_QUERY = ApiPaths.PEPPOL + "/query";
    private static final String OP_QUERY_PROVIDERS = "queryPeppolProviders";

    private static final String QUERY_PARAM_PAGE_OFFSET = "pageOffset";
    private static final String QUERY_PARAM_PAGE_SIZE = "pageSize";

    private static final int DEFAULT_PAGE_OFFSET = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final String ERR_PAGE_OFFSET_NEGATIVE = "pageOffset must be >= 0";
    private static final String ERR_PAGE_SIZE_NOT_POSITIVE = "pageSize must be > 0";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public PeppolClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Query the first page of Peppol service providers using server defaults
     * ({@code pageOffset=0}, {@code pageSize=10}).
     *
     * @return the first page of providers
     */
    @Override
    public PeppolProvidersResult query() {
        return query(DEFAULT_PAGE_OFFSET, DEFAULT_PAGE_SIZE);
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
        if (pageOffset < 0) {
            throw new IllegalArgumentException(ERR_PAGE_OFFSET_NEGATIVE);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException(ERR_PAGE_SIZE_NOT_POSITIVE);
        }
        String path = PATH_PEPPOL_QUERY
                + "?" + QUERY_PARAM_PAGE_OFFSET + "=" + pageOffset
                + "&" + QUERY_PARAM_PAGE_SIZE + "=" + pageSize;
        String token = sessionContext.token();
        QueryPeppolProvidersResponseRaw raw = http.getAuthenticated(path, token,
                QueryPeppolProvidersResponseRaw.class, OP_QUERY_PROVIDERS);
        return PeppolProvidersResult.from(raw);
    }
}
