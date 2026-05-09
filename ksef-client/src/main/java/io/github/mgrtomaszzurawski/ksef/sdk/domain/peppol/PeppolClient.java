/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;

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
     * where the consumer controls page offset and size; for full traversal
     * iterate offsets until {@link PeppolProvidersResult#hasMore()} is false.
     */
    PeppolProvidersResult query(int pageOffset, int pageSize);
}
