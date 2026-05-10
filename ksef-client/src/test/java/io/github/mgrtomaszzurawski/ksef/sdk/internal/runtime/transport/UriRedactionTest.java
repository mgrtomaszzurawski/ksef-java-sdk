/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport;

import java.net.URI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UriRedactionTest {

    @Test
    void redactNipSegments_pathWithNip_masksNip() {
        // given
        URI uri = URI.create("https://api-test.ksef.mf.gov.pl/v2/invoices/ksef/1111111111");

        // when
        String redacted = UriRedaction.redactNipSegments(uri);

        // then
        assertFalse(redacted.contains("1111111111"));
        assertTrue(redacted.contains("***1111"));
    }

    @Test
    void redactNipSegments_pathWithKsefNumber_masksFullSegment() {
        // given
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/invoices/ksef/1111111111-20260505-AB-1234");

        // when
        String redacted = UriRedaction.redactNipSegments(uri);

        // then
        assertFalse(redacted.contains("1111111111-20260505"));
        assertTrue(redacted.contains("***1234"));
    }

    @Test
    void redactNipSegments_pathWithoutNip_unchangedExceptScheme() {
        // given
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/auth/challenge");

        // when
        String redacted = UriRedaction.redactNipSegments(uri);

        // then
        assertEquals("https://api.ksef.mf.gov.pl/v2/auth/challenge", redacted);
    }

    @Test
    void redactNipSegments_uriWithQuery_redactsQuery() {
        // given
        URI uri = URI.create("https://blob.example/path?sig=secret&se=2026");

        // when
        String redacted = UriRedaction.redactNipSegments(uri);

        // then
        assertFalse(redacted.contains("sig=secret"));
        assertTrue(redacted.contains("<redacted>"));
    }

    @Test
    void redactQuery_noQuery_returnsOriginal() {
        // given
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/auth");

        // when / then
        assertEquals("https://api.ksef.mf.gov.pl/v2/auth", UriRedaction.redactQuery(uri));
    }
}
