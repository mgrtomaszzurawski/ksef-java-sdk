/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import org.jspecify.annotations.Nullable;

/**
 * Shared compact-ctor validation for {@code pageOffset} / {@code pageSize}
 * on the 8 permission query request records. KSeF enforces a
 * {@code [10, 100]} bound on every permissions query endpoint and
 * rejects out-of-range pageSize with 21405. Fail-fast at builder
 * {@code build()} time rather than at first wire call so the consumer
 * sees the offending value next to the call site.
 *
 * @since 1.0.0
 */
final class PermissionQueryPaging {

    /** Server-enforced minimum pageSize on permission query endpoints (inclusive). */
    static final int MIN_PAGE_SIZE = 10;
    /** Server-enforced maximum pageSize on permission query endpoints (inclusive). */
    static final int MAX_PAGE_SIZE = 100;

    private static final String ERR_NEGATIVE_OFFSET = "pageOffset must not be negative (got %d)";
    private static final String ERR_PAGE_SIZE_BOUNDS =
            "pageSize must be in [" + MIN_PAGE_SIZE + ", " + MAX_PAGE_SIZE + "] (got %d)";

    private PermissionQueryPaging() { }

    /**
     * Reject {@code pageOffset < 0} and {@code pageSize} outside
     * {@code [10, 100]}. Both {@code null} arguments are accepted —
     * the impl substitutes documented defaults.
     */
    static void validate(@Nullable Integer pageOffset, @Nullable Integer pageSize) {
        if (pageOffset != null && pageOffset < 0) {
            throw new IllegalArgumentException(String.format(ERR_NEGATIVE_OFFSET, pageOffset));
        }
        if (pageSize != null && (pageSize < MIN_PAGE_SIZE || pageSize > MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException(String.format(ERR_PAGE_SIZE_BOUNDS, pageSize));
        }
    }
}
