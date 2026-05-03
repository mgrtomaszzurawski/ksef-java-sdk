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
 */
public interface PeppolClient {

    PeppolProvidersResult query();
    PeppolProvidersResult query(int pageOffset, int pageSize);
}
