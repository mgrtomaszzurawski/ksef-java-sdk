/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model;

import org.jspecify.annotations.Nullable;

/**
 * Filter for single-page auth-session queries. Cursor-based paging
 * per KSeF spec: pass {@code null} for the first page; pass the
 * {@code continuationToken} from the previous {@link AuthSessionListResult}
 * to fetch the next.
 *
 * @param continuationToken cursor from the previous page; {@code null}
 *     for the first page
 * @param pageSize page size (server-side range 10–100; {@code null} =
 *     server default 10)
 *
 * @since 1.0.0
 */
public record AuthSessionsQueryRequest(
        @Nullable String continuationToken,
        @Nullable Integer pageSize) {
}
