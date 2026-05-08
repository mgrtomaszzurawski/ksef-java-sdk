/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenAuthorIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Per-method coverage for {@link TokenQueryBuilder} — the {@code GET /tokens}
 * filter does not surface every setter through a request record (the
 * SDK serialises the builder state directly into a query string), so the
 * unit tests assert the builder's own getter views and validation rules.
 */
class TokenQueryBuilderTest {

    private static final String DESCRIPTION = "issued for batch processing";
    private static final String AUTHOR_IDENTIFIER = "1234567890";
    private static final int VALID_PAGE_SIZE = 25;
    private static final int PAGE_SIZE_TOO_SMALL = 5;
    private static final int PAGE_SIZE_TOO_LARGE = 250;

    @Test
    void withStatus_addsStatusesPreservingOrder() {
        TokenQueryBuilder builder = TokenQueryBuilder.create()
                .withStatus(TokenStatus.ACTIVE, TokenStatus.REVOKED);

        assertEquals(2, builder.statuses().size());
        assertEquals(TokenStatus.ACTIVE, builder.statuses().get(0));
        assertEquals(TokenStatus.REVOKED, builder.statuses().get(1));
    }

    @Test
    void description_setsValueRetrievableViaGetter() {
        TokenQueryBuilder builder = TokenQueryBuilder.create().description(DESCRIPTION);

        assertEquals(DESCRIPTION, builder.descriptionValue());
    }

    @Test
    void description_whenTooShort_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> TokenQueryBuilder.create().description("ab"));
    }

    @Test
    void authorIdentifier_setsValueRetrievableViaGetter() {
        TokenQueryBuilder builder = TokenQueryBuilder.create().authorIdentifier(AUTHOR_IDENTIFIER);

        assertEquals(AUTHOR_IDENTIFIER, builder.authorIdentifierValue());
    }

    @Test
    void authorIdentifierType_setsValueRetrievableViaGetter() {
        TokenQueryBuilder builder = TokenQueryBuilder.create()
                .authorIdentifierType(TokenAuthorIdentifierType.NIP);

        assertEquals(TokenAuthorIdentifierType.NIP, builder.authorIdentifierTypeValue());
    }

    @Test
    void pageSize_setsValueRetrievableViaGetter() {
        TokenQueryBuilder builder = TokenQueryBuilder.create().pageSize(VALID_PAGE_SIZE);

        assertEquals(VALID_PAGE_SIZE, builder.pageSizeValue());
    }

    @Test
    void pageSize_whenOutOfRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> TokenQueryBuilder.create().pageSize(PAGE_SIZE_TOO_SMALL));
        assertThrows(IllegalArgumentException.class,
                () -> TokenQueryBuilder.create().pageSize(PAGE_SIZE_TOO_LARGE));
    }

    @Test
    void toBuilder_returnsIndependentCopyOfState() {
        TokenQueryBuilder original = TokenQueryBuilder.create()
                .withStatus(TokenStatus.ACTIVE)
                .description(DESCRIPTION)
                .authorIdentifier(AUTHOR_IDENTIFIER)
                .authorIdentifierType(TokenAuthorIdentifierType.NIP)
                .pageSize(VALID_PAGE_SIZE);

        TokenQueryBuilder copy = original.toBuilder();

        assertNotSame(original, copy);
        assertTrue(copy.statuses().contains(TokenStatus.ACTIVE));
        assertEquals(DESCRIPTION, copy.descriptionValue());
        assertEquals(AUTHOR_IDENTIFIER, copy.authorIdentifierValue());
        assertEquals(TokenAuthorIdentifierType.NIP, copy.authorIdentifierTypeValue());
        assertEquals(VALID_PAGE_SIZE, copy.pageSizeValue());
    }
}
