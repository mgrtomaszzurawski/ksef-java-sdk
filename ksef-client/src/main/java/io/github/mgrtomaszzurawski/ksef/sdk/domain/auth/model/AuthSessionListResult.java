/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Single-page result of an auth-session query.
 *
 * <p>Unlike the offset-based {@code XxxQueryResult} records elsewhere in
 * the SDK (which carry {@code hasMore} + {@code totalCount}), KSeF's
 * {@code GET /auth/sessions} endpoint paginates via the
 * {@code x-continuation-token} header. Presence of a non-null
 * {@code continuationToken} signals more pages are available; consumers
 * pass it back via {@link AuthSessionsQueryRequest#continuationToken()}
 * to fetch the next page.
 *
 * @param sessions auth sessions on this page
 * @param continuationToken cursor to fetch the next page, or {@code null}
 *     when this is the last page
 *
 * @since 0.1.0
 */
public record AuthSessionListResult(
        List<AuthSession> sessions,
        @Nullable String continuationToken) {

    public AuthSessionListResult {
        sessions = List.copyOf(sessions);
    }

    /** Whether more pages remain (i.e. the continuation token is non-null). */
    public boolean hasMore() {
        return continuationToken != null;
    }
}
