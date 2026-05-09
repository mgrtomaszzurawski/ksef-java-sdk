/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.CommonSessionStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pin every {@link CommonSessionStatus#wireValue()} mapping against the
 * spec-defined string. The mapping is what {@code SessionClient} emits
 * on the wire as the {@code statuses} query parameter on {@code GET /sessions};
 * a typo in any constant's wireValue would silently send the wrong filter
 * to KSeF and produce empty / mis-filtered result sets.
 *
 * <p>Source of truth: OpenAPI {@code CommonSessionStatus} string enum
 * (open-api.json). Values: {@code "InProgress"}, {@code "Succeeded"},
 * {@code "Failed"}, {@code "Cancelled"} — note British spelling on the
 * last constant.
 */
class CommonSessionStatusTest {

    @Test
    void wireValue_inProgress_mapsToOpenApiSpecString() {
        assertEquals("InProgress", CommonSessionStatus.IN_PROGRESS.wireValue());
    }

    @Test
    void wireValue_succeeded_mapsToOpenApiSpecString() {
        assertEquals("Succeeded", CommonSessionStatus.SUCCEEDED.wireValue());
    }

    @Test
    void wireValue_failed_mapsToOpenApiSpecString() {
        assertEquals("Failed", CommonSessionStatus.FAILED.wireValue());
    }

    @Test
    void wireValue_cancelled_mapsToOpenApiSpecString() {
        assertEquals("Cancelled", CommonSessionStatus.CANCELLED.wireValue());
    }
}
