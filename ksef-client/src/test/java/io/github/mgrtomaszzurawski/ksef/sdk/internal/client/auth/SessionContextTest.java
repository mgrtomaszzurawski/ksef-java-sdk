/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionContextTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.test";
    private static final String REFERENCE_NUMBER = "20260403-REF-001";
    private static final String REFRESHED_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refreshed";

    private SessionContext context;

    @BeforeEach
    void setUp() {
        context = new SessionContext();
    }

    @Test
    void isActive_whenNew_returnsFalse() {
        assertFalse(context.isActive());
    }

    @Test
    void isExpired_whenNew_returnsTrue() {
        assertTrue(context.isExpired());
    }

    @Test
    void token_whenNoSession_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> context.token());
    }

    @Test
    void referenceNumber_whenNoSession_throwsIllegalState() {
        assertThrows(IllegalStateException.class, () -> context.referenceNumber());
    }

    @Test
    void activate_setsSessionState() {
        // given
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);

        // when
        context.activate(TOKEN, REFERENCE_NUMBER, expiry);

        // then
        assertTrue(context.isActive());
        assertFalse(context.isExpired());
        assertEquals(TOKEN, context.token());
        assertEquals(REFERENCE_NUMBER, context.referenceNumber());
    }

    @Test
    void updateAccessToken_updatesTokenKeepsReference() {
        // given
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);
        context.activate(TOKEN, REFERENCE_NUMBER, expiry);

        OffsetDateTime newExpiry = OffsetDateTime.now().plusHours(2);

        // when
        context.updateAccessToken(REFRESHED_TOKEN, newExpiry);

        // then
        assertEquals(REFRESHED_TOKEN, context.token());
        assertEquals(REFERENCE_NUMBER, context.referenceNumber());
    }

    @Test
    void updateAccessToken_whenNoSession_throwsIllegalState() {
        // given
        OffsetDateTime expiry = OffsetDateTime.now().plusHours(1);

        // then
        assertThrows(IllegalStateException.class, () -> context.updateAccessToken(REFRESHED_TOKEN, expiry));
    }

    @Test
    void clear_removesSession() {
        // given
        context.activate(TOKEN, REFERENCE_NUMBER, OffsetDateTime.now().plusHours(1));

        // when
        context.clear();

        // then
        assertFalse(context.isActive());
        assertTrue(context.isExpired());
    }

    @Test
    void isExpired_whenPastExpiry_returnsTrue() {
        // given
        OffsetDateTime pastExpiry = OffsetDateTime.now().minusMinutes(1);

        // when
        context.activate(TOKEN, REFERENCE_NUMBER, pastExpiry);

        // then
        assertTrue(context.isExpired());
    }
}
