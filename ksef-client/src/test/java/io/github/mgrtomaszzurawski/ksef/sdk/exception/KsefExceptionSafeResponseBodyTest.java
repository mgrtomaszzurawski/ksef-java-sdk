/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KsefExceptionSafeResponseBodyTest {

    private static final String MESSAGE = "test";
    private static final int STATUS_400 = 400;
    private static final int STATUS_500 = 500;

    @Test
    void safeResponseBody_whenBodyIsNull_returnsNull() {
        KsefException ex = new KsefException(MESSAGE, null, STATUS_400, null);
        assertNull(ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenNipPresent_replacesAllButLastFour() {
        KsefException ex = new KsefException(MESSAGE, null, STATUS_400,
                "{\"nip\":\"9876543210\"}");
        assertEquals("{\"nip\":\"***3210\"}", ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenPeselPresent_replacesAllButLastFour() {
        KsefException ex = new KsefException(MESSAGE, null, STATUS_400,
                "{\"pesel\":\"82010198765\"}");
        assertEquals("{\"pesel\":\"***8765\"}", ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenShortDigitRun_leavesUnchanged() {
        KsefException ex = new KsefException(MESSAGE, null, STATUS_400,
                "{\"ordinal\":1234}");
        assertEquals("{\"ordinal\":1234}", ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenJwtPresent_collapsesToHeaderAndSignatureTail() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signaturePartThatIsLong1234";
        KsefException ex = new KsefException(MESSAGE, null, STATUS_500,
                "{\"token\":\"" + jwt + "\"}");
        // JWT collapsed to eyJ.***<last 4 chars of signature segment>
        assertEquals("{\"token\":\"eyJ.***1234\"}", ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenMixedJwtAndNip_scrubsBoth() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJ4In0.sigaaaa9999";
        KsefException ex = new KsefException(MESSAGE, null, STATUS_500,
                "{\"token\":\"" + jwt + "\",\"nip\":\"1112223334\"}");
        assertEquals("{\"token\":\"eyJ.***9999\",\"nip\":\"***3334\"}",
                ex.safeResponseBody());
    }

    @Test
    void safeResponseBody_whenNoSensitiveData_returnsUnchanged() {
        KsefException ex = new KsefException(MESSAGE, null, STATUS_500,
                "{\"description\":\"plain error message\"}");
        assertEquals("{\"description\":\"plain error message\"}", ex.safeResponseBody());
    }
}
