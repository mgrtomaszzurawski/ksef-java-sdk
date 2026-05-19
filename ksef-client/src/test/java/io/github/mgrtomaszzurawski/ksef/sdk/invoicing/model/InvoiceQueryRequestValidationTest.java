/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link InvoiceQueryRequest} server-bound validation (added with
 * the new {@code pageOffset} / {@code pageSize} record components):
 * negative offsets rejected, page-size bounded to the OpenAPI-spec
 * range [10, 250]. Builder fluent-setter round-trips also verified.
 */
class InvoiceQueryRequestValidationTest {

    private static final int MIN_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 250;
    private static final OffsetDateTime DATE_FROM = OffsetDateTime.of(
            2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void compactConstructor_whenPageOffsetNegative_throwsIllegalArgument() {
        InvoiceQueryBuilder builder = baseBuilder().pageOffset(-1);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(thrown.getMessage().contains("-1"),
                () -> "Error must reference the offending value: " + thrown.getMessage());
    }

    @Test
    void compactConstructor_whenPageSizeBelowMin_throwsIllegalArgument() {
        InvoiceQueryBuilder builder = baseBuilder().pageSize(MIN_PAGE_SIZE - 1);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(thrown.getMessage().contains(Integer.toString(MIN_PAGE_SIZE - 1)),
                () -> "Error must reference the offending value: " + thrown.getMessage());
    }

    @Test
    void compactConstructor_whenPageSizeAboveMax_throwsIllegalArgument() {
        InvoiceQueryBuilder builder = baseBuilder().pageSize(MAX_PAGE_SIZE + 1);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(thrown.getMessage().contains(Integer.toString(MAX_PAGE_SIZE + 1)),
                () -> "Error must reference the offending value: " + thrown.getMessage());
    }

    @Test
    void compactConstructor_whenPageSizeAtLowerBound_accepted() {
        InvoiceQueryRequest request = baseBuilder().pageSize(MIN_PAGE_SIZE).build();
        assertEquals(MIN_PAGE_SIZE, request.pageSize());
    }

    @Test
    void compactConstructor_whenPageSizeAtUpperBound_accepted() {
        InvoiceQueryRequest request = baseBuilder().pageSize(MAX_PAGE_SIZE).build();
        assertEquals(MAX_PAGE_SIZE, request.pageSize());
    }

    @Test
    void compactConstructor_whenPageOffsetZero_accepted() {
        InvoiceQueryRequest request = baseBuilder().pageOffset(0).build();
        assertEquals(0, request.pageOffset());
    }

    @Test
    void builder_whenPageFieldsUnset_recordCarriesNulls() {
        InvoiceQueryRequest request = baseBuilder().build();
        assertNull(request.pageOffset(),
                "Unset pageOffset must remain null so the impl can apply its default.");
        assertNull(request.pageSize(),
                "Unset pageSize must remain null so the impl can apply its default.");
    }

    @Test
    void builder_pageOffsetAndPageSize_roundTripThroughBuild() {
        InvoiceQueryRequest request = baseBuilder().pageOffset(7).pageSize(50).build();
        assertEquals(7, request.pageOffset());
        assertEquals(50, request.pageSize());
    }

    private static InvoiceQueryBuilder baseBuilder() {
        return InvoiceQueryBuilder.seller().invoicingDateFrom(DATE_FROM);
    }
}
