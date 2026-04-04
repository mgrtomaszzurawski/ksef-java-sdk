/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.paging;

import java.util.List;

/**
 * Represents a single page of results from a KSeF API paginated endpoint.
 * KSeF uses offset-based pagination with pageOffset and pageSize query parameters.
 *
 * @param <T> the type of items in the page
 */
public record PagedResponse<T>(
        List<T> items,
        int pageOffset,
        int pageSize,
        boolean hasMore) {

    public PagedResponse {
        items = List.copyOf(items);
    }

    /**
     * Calculate the offset for the next page.
     */
    public int nextPageOffset() {
        return pageOffset + pageSize;
    }
}
