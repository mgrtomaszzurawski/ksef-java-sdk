/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvider;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import java.util.stream.Stream;

/**
 * Client for KSeF Peppol service provider queries.
 *
 * <p>Lists Peppol service providers registered in KSeF. Results are sorted by
 * {@code dateCreated} descending, then {@code id} ascending. Requires authentication.
 *
 * @since 1.0.0
 */
public interface PeppolClient {

    /**
     * Single page of Peppol providers. Use this for explicit UI pagination
     * where the consumer controls page offset and size. For full traversal
     * prefer {@link #streamProviders()}, which lazily walks pages until the
     * server reports {@code hasMore == false}.
     */
    PeppolProvidersResult query(int pageOffset, int pageSize);

    /**
     * Stream every Peppol provider registered in KSeF. Pages are fetched
     * lazily via {@code pageOffset = 0, 1, 2, ...} until the server reports
     * {@code hasMore == false}. The SDK never materialises the full list —
     * memory pressure is bounded by what the caller pulls from the stream.
     * For a hard cap pipe through {@code .limit(N)}; for a snapshot list
     * pipe through {@code .toList()}.
     */
    Stream<PeppolProvider> streamProviders();
}
