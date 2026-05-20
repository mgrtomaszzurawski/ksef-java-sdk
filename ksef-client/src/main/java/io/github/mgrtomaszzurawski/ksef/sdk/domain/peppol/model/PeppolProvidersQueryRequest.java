/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

import java.util.Objects;

/**
 * Paginated Peppol providers query. Named fields replace the raw
 * {@code (int pageOffset, int pageSize)} pair so callers cannot
 * silently swap argument positions.
 *
 * @param pageOffset zero-based page offset (non-negative)
 * @param pageSize   page size (positive)
 *
 * @since 0.1.0
 */
public record PeppolProvidersQueryRequest(int pageOffset, int pageSize) {

    private static final String ERR_NEGATIVE_OFFSET = "pageOffset must not be negative";
    private static final String ERR_NON_POSITIVE_SIZE = "pageSize must be positive";

    public PeppolProvidersQueryRequest {
        if (pageOffset < 0) {
            throw new IllegalArgumentException(ERR_NEGATIVE_OFFSET);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException(ERR_NON_POSITIVE_SIZE);
        }
    }

    /** Builder-style factory for the common first-page lookup. */
    public static PeppolProvidersQueryRequest firstPage(int pageSize) {
        return new PeppolProvidersQueryRequest(0, pageSize);
    }

    @Override
    public String toString() {
        return "PeppolProvidersQueryRequest[pageOffset=" + pageOffset + ", pageSize=" + pageSize + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PeppolProvidersQueryRequest that)) {
            return false;
        }
        return pageOffset == that.pageOffset && pageSize == that.pageSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageOffset, pageSize);
    }
}
