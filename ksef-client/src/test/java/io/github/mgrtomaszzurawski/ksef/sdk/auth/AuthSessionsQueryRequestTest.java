/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionsQueryRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link AuthSessionsQueryRequest#firstPage(int)} server-bound
 * validation (10-100 inclusive per KSeF spec) and the
 * {@link AuthSessionsQueryRequest#defaults() defaults} shape.
 */
class AuthSessionsQueryRequestTest {

    private static final int MIN_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    @Test
    void defaults_carriesNullCursorAndNullPageSize() {
        AuthSessionsQueryRequest request = AuthSessionsQueryRequest.defaults();
        assertNull(request.continuationToken());
        assertNull(request.pageSize());
    }

    @Test
    void firstPage_atLowerBound_carriesPageSizeWithNullCursor() {
        AuthSessionsQueryRequest request = AuthSessionsQueryRequest.firstPage(MIN_PAGE_SIZE);
        assertNull(request.continuationToken());
        assertEquals(MIN_PAGE_SIZE, request.pageSize());
    }

    @Test
    void firstPage_atUpperBound_carriesPageSizeWithNullCursor() {
        AuthSessionsQueryRequest request = AuthSessionsQueryRequest.firstPage(MAX_PAGE_SIZE);
        assertNull(request.continuationToken());
        assertEquals(MAX_PAGE_SIZE, request.pageSize());
    }

    @Test
    void firstPage_inRange_carriesPageSize() {
        AuthSessionsQueryRequest request = AuthSessionsQueryRequest.firstPage(50);
        assertEquals(50, request.pageSize());
    }

    @Test
    void firstPage_belowMin_throwsIllegalArgument() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> AuthSessionsQueryRequest.firstPage(MIN_PAGE_SIZE - 1));
        assertTrue(thrown.getMessage().contains(Integer.toString(MIN_PAGE_SIZE - 1)),
                () -> "Error message should reference the offending value: " + thrown.getMessage());
    }

    @Test
    void firstPage_aboveMax_throwsIllegalArgument() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> AuthSessionsQueryRequest.firstPage(MAX_PAGE_SIZE + 1));
        assertTrue(thrown.getMessage().contains(Integer.toString(MAX_PAGE_SIZE + 1)),
                () -> "Error message should reference the offending value: " + thrown.getMessage());
    }

    @Test
    void firstPage_zero_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthSessionsQueryRequest.firstPage(0));
    }

    @Test
    void firstPage_negative_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> AuthSessionsQueryRequest.firstPage(-1));
    }
}
