/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.certificates.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Per-method coverage for {@link CertificateQueryBuilder#pageOffset(int)}
 * and {@link CertificateQueryBuilder#pageSize(int)} — the paging knobs are
 * not stamped into {@code CertificateQueryRequest}, so they are exercised
 * via the builder's own getters.
 */
class CertificateQueryBuilderTest {

    private static final int PAGE_OFFSET = 3;
    private static final int PAGE_SIZE = 50;

    @Test
    void pageOffset_setsValueRetrievableViaGetter() {
        CertificateQueryBuilder builder = CertificateQueryBuilder.create().pageOffset(PAGE_OFFSET);

        assertEquals(PAGE_OFFSET, builder.pageOffsetValue());
        assertNull(builder.pageSizeValue());
    }

    @Test
    void pageSize_setsValueRetrievableViaGetter() {
        CertificateQueryBuilder builder = CertificateQueryBuilder.create().pageSize(PAGE_SIZE);

        assertEquals(PAGE_SIZE, builder.pageSizeValue());
        assertNull(builder.pageOffsetValue());
    }
}
