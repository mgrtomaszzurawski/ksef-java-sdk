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
        URI uri = URI.create("https://api-test.ksef.mf.gov.pl/v2/invoices/ksef/1234567890");
        String redacted = UriRedaction.redactNipSegments(uri);
        assertFalse(redacted.contains("1234567890"));
        assertTrue(redacted.contains("***7890"));
    }

    @Test
    void redactNipSegments_pathWithKsefNumber_masksFullSegment() {
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/invoices/ksef/1234567890-20260505-AB-1234");
        String redacted = UriRedaction.redactNipSegments(uri);
        assertFalse(redacted.contains("1234567890-20260505"));
        assertTrue(redacted.contains("***1234"));
    }

    @Test
    void redactNipSegments_pathWithoutNip_unchangedExceptScheme() {
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/auth/challenge");
        String redacted = UriRedaction.redactNipSegments(uri);
        assertEquals("https://api.ksef.mf.gov.pl/v2/auth/challenge", redacted);
    }

    @Test
    void redactNipSegments_uriWithQuery_redactsQuery() {
        URI uri = URI.create("https://blob.example/path?sig=secret&se=2026");
        String redacted = UriRedaction.redactNipSegments(uri);
        assertFalse(redacted.contains("sig=secret"));
        assertTrue(redacted.contains("<redacted>"));
    }

    @Test
    void redactQuery_noQuery_returnsOriginal() {
        URI uri = URI.create("https://api.ksef.mf.gov.pl/v2/auth");
        assertEquals("https://api.ksef.mf.gov.pl/v2/auth", UriRedaction.redactQuery(uri));
    }
}
