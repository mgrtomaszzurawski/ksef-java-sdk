/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.transport;

import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the RFC 7231 §7.1.1.1 {@code Retry-After} parser inside
 * {@code HttpSupport.parseRetryAfterValue}. RFC permits three date formats:
 * <ol>
 *   <li>RFC 1123 / RFC 5322 (e.g. {@code "Sun, 06 Nov 1994 08:49:37 GMT"}),</li>
 *   <li>RFC 850 / obsolete (e.g. {@code "Sunday, 06-Nov-94 08:49:37 GMT"}),</li>
 *   <li>asctime (e.g. {@code "Sun Nov  6 08:49:37 1994"}).</li>
 * </ol>
 *
 * <p>Plus integer delta-seconds. The parser is private static; tests reach
 * it via reflection because the rest of the SDK only exposes the parsed
 * delta indirectly through {@link io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.RetryHandler}.
 *
 * <p>Covers TC-LIMIT-004 and TC-LIMIT-005 from the QA backlog.
 */
class RetryAfterParserTest {

    private static final long FUTURE_DELTA_SECONDS = 120L;

    @Test
    void parser_acceptsIntegerDeltaSeconds() throws Exception {
        Optional<Long> result = invokeParseRetryAfterValue("3");

        assertTrue(result.isPresent(), "Integer delta-seconds must parse");
        assertEquals(3L, result.get());
    }

    @Test
    void parser_acceptsZeroSeconds() throws Exception {
        Optional<Long> result = invokeParseRetryAfterValue("0");

        assertTrue(result.isPresent());
        assertEquals(0L, result.get());
    }

    @Test
    void parser_rejectsNegativeIntegerDeltaSeconds() throws Exception {
        Optional<Long> result = invokeParseRetryAfterValue("-5");

        assertTrue(result.isEmpty(), "Negative delta-seconds must NOT parse");
    }

    @Test
    void parser_acceptsRfc1123DateFormat() throws Exception {
        // Build an HTTP-date that is N seconds in the future and assert the parser
        // returns approximately N (allow 2-second drift between formatting and the
        // parser observing "now").
        OffsetDateTime future = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(FUTURE_DELTA_SECONDS);
        String httpDate = future.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        Optional<Long> result = invokeParseRetryAfterValue(httpDate);

        assertTrue(result.isPresent(), "RFC 1123 HTTP-date must parse: " + httpDate);
        assertTrue(result.get() >= FUTURE_DELTA_SECONDS - 2 && result.get() <= FUTURE_DELTA_SECONDS,
                "Parsed delta must be ~" + FUTURE_DELTA_SECONDS + "s; got " + result.get());
    }

    @Test
    void parser_acceptsRfc850DateFormat() throws Exception {
        // Spec example from RFC 7231: "Sunday, 06-Nov-94 08:49:37 GMT" — but dates
        // before now collapse to delta=0. Use a future date so the test is meaningful.
        // RFC 850 uses {@code zzz} which requires a {@link java.time.ZonedDateTime}
        // (with a named zone), not a {@link OffsetDateTime}.
        java.time.ZonedDateTime future = java.time.ZonedDateTime.now(java.time.ZoneId.of("GMT"))
                .plusSeconds(FUTURE_DELTA_SECONDS);
        DateTimeFormatter rfc850 = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", java.util.Locale.ROOT);
        String httpDate = future.format(rfc850);

        Optional<Long> result = invokeParseRetryAfterValue(httpDate);

        assertTrue(result.isPresent(), "RFC 850 HTTP-date must parse: " + httpDate);
        assertTrue(result.get() >= FUTURE_DELTA_SECONDS - 2 && result.get() <= FUTURE_DELTA_SECONDS);
    }

    @Test
    void parser_acceptsAsctimeDateFormat() throws Exception {
        // RFC 7231 third permitted format: ANSI C asctime (no zone — interpreted as GMT).
        // Spec example shape: "Sun Nov  6 08:49:37 1994" — note the double space when
        // day-of-month is single-digit. The HttpSupport asctime formatter matches this.
        // Pick a future date so the parsed delta is positive.
        java.time.ZonedDateTime future = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .plusSeconds(FUTURE_DELTA_SECONDS);
        DateTimeFormatter asctime = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy",
                java.util.Locale.ROOT);
        String httpDate = future.format(asctime);

        Optional<Long> result = invokeParseRetryAfterValue(httpDate);

        assertTrue(result.isPresent(), "asctime HTTP-date must parse: " + httpDate);
        assertTrue(result.get() >= FUTURE_DELTA_SECONDS - 2 && result.get() <= FUTURE_DELTA_SECONDS,
                "Parsed delta must be ~" + FUTURE_DELTA_SECONDS + "s; got " + result.get());
    }

    @Test
    void parser_collapsesPastHttpDateToZero() throws Exception {
        // Spec: past dates produce delta=0, not negative.
        String pastDate = "Sun, 06 Nov 1994 08:49:37 GMT";

        Optional<Long> result = invokeParseRetryAfterValue(pastDate);

        assertTrue(result.isPresent(), "Past HTTP-date must parse, not error");
        assertEquals(0L, result.get(), "Past HTTP-date must collapse to 0 seconds");
    }

    @Test
    void parser_rejectsMalformedValues() throws Exception {
        Optional<Long> garbage = invokeParseRetryAfterValue("not-a-date-or-number");
        Optional<Long> empty = invokeParseRetryAfterValue("");

        assertTrue(garbage.isEmpty(), "Garbage value must NOT parse");
        assertTrue(empty.isEmpty(), "Empty value must NOT parse");
    }

    @Test
    void parser_trimsLeadingTrailingWhitespace() throws Exception {
        Optional<Long> result = invokeParseRetryAfterValue("  10  ");

        assertTrue(result.isPresent());
        assertEquals(10L, result.get());
    }

    @SuppressWarnings("unchecked")
    private static Optional<Long> invokeParseRetryAfterValue(String value) throws Exception {
        Method method = HttpSupport.class.getDeclaredMethod("parseRetryAfterValue", String.class);
        method.setAccessible(true);
        return (Optional<Long>) method.invoke(null, value);
    }
}
