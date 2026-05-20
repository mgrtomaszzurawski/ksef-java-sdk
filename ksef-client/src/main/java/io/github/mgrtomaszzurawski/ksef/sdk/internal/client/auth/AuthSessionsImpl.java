/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.AuthSessions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionListResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.AuthenticationListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.pagination.PagedSpliterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Package-private implementation of {@link AuthSessions}. Constructed by
 * {@code KsefClient}; delegates to {@link AuthClient} for HTTP work and
 * threads in the parent client's lifecycle hooks (gate-checks,
 * lazy-auth trigger, terminate-side cleanup, last-challenge IP).
 *
 * @since 0.1.0
 */
public final class AuthSessionsImpl implements AuthSessions {

    private static final String ERR_REF_NULL = "referenceNumber must not be null";
    private static final String ERR_NULL_FILTER = "filter must not be null";
    private static final String ERR_NO_IP_AFTER_AUTH =
            "Auth completed but clientIp not populated — SDK internal error";

    private final AuthClient authClient;
    private final Runnable ensureOpen;
    private final Runnable ensureAuthenticated;
    private final Runnable onTerminate;
    private final Supplier<Optional<String>> lastChallengeClientIpSupplier;

    public AuthSessionsImpl(AuthClient authClient,
                    Runnable ensureOpen,
                    Runnable ensureAuthenticated,
                    Runnable onTerminate,
                    Supplier<Optional<String>> lastChallengeClientIpSupplier) {
        this.authClient = Objects.requireNonNull(authClient, "authClient");
        this.ensureOpen = Objects.requireNonNull(ensureOpen, "ensureOpen");
        this.ensureAuthenticated = Objects.requireNonNull(ensureAuthenticated, "ensureAuthenticated");
        this.onTerminate = Objects.requireNonNull(onTerminate, "onTerminate");
        this.lastChallengeClientIpSupplier =
                Objects.requireNonNull(lastChallengeClientIpSupplier, "lastChallengeClientIpSupplier");
    }

    @Override
    public void ensureLoggedIn() {
        ensureOpen.run();
        ensureAuthenticated.run();
    }

    @Override
    public void terminate() {
        ensureOpen.run();
        try {
            authClient.terminateCurrentSession();
        } finally {
            // Local state cleanup is best-effort and idempotent — runs
            // regardless of HTTP outcome. If the server DELETE failed,
            // any future protected call retries auth via the 401-driven
            // reauth path; the local "authenticated=false" flag here
            // just primes lazy auth correctly.
            onTerminate.run();
        }
    }

    @Override
    public AuthSessionListResult queryAuthSessions(AuthSessionsQueryRequest filter) {
        ensureOpen.run();
        ensureAuthenticated.run();
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        var page = authClient.listSessions(filter.continuationToken(), filter.pageSize());
        List<AuthSession> mapped = page.items().stream().map(AuthSessionsImpl::toAuthSession).toList();
        return new AuthSessionListResult(mapped, page.continuationToken());
    }

    @Override
    public Stream<AuthSession> streamAuthSessions(AuthSessionsQueryRequest filter) {
        ensureOpen.run();
        ensureAuthenticated.run();
        Objects.requireNonNull(filter, ERR_NULL_FILTER);
        Integer pageSize = filter.pageSize();
        return PagedSpliterator.cursorStream(continuationToken -> {
            var page = authClient.listSessions(continuationToken, pageSize);
            List<AuthSession> mapped = page.items().stream().map(AuthSessionsImpl::toAuthSession).toList();
            return new PagedSpliterator.CursorPage<>(mapped, page.continuationToken());
        });
    }

    @Override
    public void terminateSession(String referenceNumber) {
        ensureOpen.run();
        ensureAuthenticated.run();
        authClient.terminateSession(Objects.requireNonNull(referenceNumber, ERR_REF_NULL));
    }

    @Override
    public String lastChallengeClientIp() {
        ensureOpen.run();
        ensureAuthenticated.run();
        return lastChallengeClientIpSupplier.get()
                .orElseThrow(() -> new IllegalStateException(ERR_NO_IP_AFTER_AUTH));
    }

    private static AuthSession toAuthSession(AuthenticationListItem item) {
        return new AuthSession(
                item.referenceNumber(),
                item.startDate(),
                item.authenticationMethodInfo() == null
                        ? null : item.authenticationMethodInfo().displayName(),
                item.status(),
                Boolean.TRUE.equals(item.tokenRedeemed()),
                item.lastTokenRefreshDate(),
                item.refreshTokenValidUntil(),
                Boolean.TRUE.equals(item.current()));
    }
}
