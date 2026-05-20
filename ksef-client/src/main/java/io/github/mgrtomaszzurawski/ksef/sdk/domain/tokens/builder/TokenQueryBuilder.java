/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenAuthorIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for the {@code GET /tokens} list filter (per spec
 * {@code ksef-docs/tokeny-ksef.md} — five optional query parameters
 * + the {@code x-continuation-token} cursor handled by the SDK).
 *
 * <p>All fields are optional; a builder with no setters invoked
 * matches every token visible to the authenticated principal.
 *
 * @since 0.1.0
 */
public final class TokenQueryBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 3;
    private static final int AUTHOR_IDENTIFIER_MIN_LENGTH = 3;
    private static final int PAGE_SIZE_MIN = 10;
    private static final int PAGE_SIZE_MAX = 100;

    private static final String ERR_DESCRIPTION_TOO_SHORT =
            "description must be at least " + DESCRIPTION_MIN_LENGTH + " characters";
    private static final String ERR_AUTHOR_IDENTIFIER_TOO_SHORT =
            "authorIdentifier must be at least " + AUTHOR_IDENTIFIER_MIN_LENGTH + " characters";
    private static final String ERR_PAGE_SIZE_OUT_OF_RANGE =
            "pageSize must be in range [" + PAGE_SIZE_MIN + ", " + PAGE_SIZE_MAX + "]";
    private static final String ERR_NULL_STATUS = "status must not be null";

    private final List<TokenStatus> statuses = new ArrayList<>();
    private @Nullable String description;
    private @Nullable String authorIdentifier;
    private @Nullable TokenAuthorIdentifierType authorIdentifierType;
    private @Nullable String continuationToken;
    private @Nullable Integer pageSize;

    private TokenQueryBuilder() { }

    /** Create an empty filter — equivalent to {@code GET /tokens} with no query params. */
    public static TokenQueryBuilder create() {
        return new TokenQueryBuilder();
    }

    /**
     * Add one or more {@link TokenStatus} values to the filter
     * (multi-value query parameter {@code status}).
     */
    public TokenQueryBuilder withStatus(TokenStatus... values) {
        for (TokenStatus value : values) {
            this.statuses.add(Objects.requireNonNull(value, ERR_NULL_STATUS));
        }
        return this;
    }

    /**
     * Set the {@code description} substring filter (case-insensitive,
     * minimum {@value #DESCRIPTION_MIN_LENGTH} characters per spec).
     */
    public TokenQueryBuilder description(String description) {
        Objects.requireNonNull(description, "description");
        if (description.length() < DESCRIPTION_MIN_LENGTH) {
            throw new IllegalArgumentException(ERR_DESCRIPTION_TOO_SHORT);
        }
        this.description = description;
        return this;
    }

    /**
     * Set the {@code authorIdentifier} substring filter (case-insensitive,
     * minimum {@value #AUTHOR_IDENTIFIER_MIN_LENGTH} characters per spec).
     */
    public TokenQueryBuilder authorIdentifier(String authorIdentifier) {
        Objects.requireNonNull(authorIdentifier, "authorIdentifier");
        if (authorIdentifier.length() < AUTHOR_IDENTIFIER_MIN_LENGTH) {
            throw new IllegalArgumentException(ERR_AUTHOR_IDENTIFIER_TOO_SHORT);
        }
        this.authorIdentifier = authorIdentifier;
        return this;
    }

    /**
     * Set the {@code authorIdentifierType} qualifier — usually paired
     * with {@link #authorIdentifier(String)} so the server knows whether
     * the supplied identifier is a NIP, PESEL, or fingerprint.
     */
    public TokenQueryBuilder authorIdentifierType(TokenAuthorIdentifierType type) {
        this.authorIdentifierType = Objects.requireNonNull(type, "authorIdentifierType");
        return this;
    }

    /**
     * Continue an explicit-pagination walk by supplying the
     * {@code continuationToken} returned by the previous
     * {@code queryTokens} call's
     * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenList#continuationToken()}.
     * Not set (or {@code null}) means start from the beginning.
     * Ignored by {@code streamTokens}.
     */
    public TokenQueryBuilder continuationToken(String continuationToken) {
        this.continuationToken = Objects.requireNonNull(continuationToken, "continuationToken");
        return this;
    }

    /**
     * Set the page size (server-side, {@value #PAGE_SIZE_MIN}-{@value #PAGE_SIZE_MAX}).
     * The SDK {@code streamTokens} paginator handles the
     * {@code x-continuation-token} cursor automatically; explicit
     * navigation through {@code queryTokens} feeds the cursor back via
     * {@link #continuationToken(String)}.
     */
    public TokenQueryBuilder pageSize(int pageSize) {
        if (pageSize < PAGE_SIZE_MIN || pageSize > PAGE_SIZE_MAX) {
            throw new IllegalArgumentException(ERR_PAGE_SIZE_OUT_OF_RANGE);
        }
        this.pageSize = pageSize;
        return this;
    }

    public List<TokenStatus> statuses() {
        return List.copyOf(statuses);
    }

    public @Nullable String descriptionValue() {
        return description;
    }

    public @Nullable String authorIdentifierValue() {
        return authorIdentifier;
    }

    public @Nullable TokenAuthorIdentifierType authorIdentifierTypeValue() {
        return authorIdentifierType;
    }

    public @Nullable Integer pageSizeValue() {
        return pageSize;
    }

    /**
     * Currently configured continuation cursor, or {@code null} when
     * the builder is set to fetch the first page. Symmetric accessor
     * to {@link #continuationToken(String)}; provided to support
     * builder-state inspection in higher-level orchestration code.
     */
    public @Nullable String continuationTokenValue() {
        return continuationToken;
    }

    /**
     * Build the immutable {@link TokenQueryRequest} captured by this builder.
     */
    public TokenQueryRequest build() {
        return new TokenQueryRequest(statuses, description, authorIdentifier, authorIdentifierType,
                continuationToken, pageSize);
    }

    /** Return a copy of this builder with the same field state (ergonomic for partial reuse). */
    public TokenQueryBuilder toBuilder() {
        TokenQueryBuilder copy = new TokenQueryBuilder();
        copy.statuses.addAll(this.statuses);
        copy.description = this.description;
        copy.authorIdentifier = this.authorIdentifier;
        copy.authorIdentifierType = this.authorIdentifierType;
        copy.continuationToken = this.continuationToken;
        copy.pageSize = this.pageSize;
        return copy;
    }
}
