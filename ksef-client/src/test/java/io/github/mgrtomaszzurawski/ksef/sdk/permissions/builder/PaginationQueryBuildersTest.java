/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Per-method coverage for the four permissions paging-only query builders:
 * {@link EntityRolesQueryBuilder}, {@link SubordinateEntityRolesQueryBuilder},
 * {@link SubunitPermissionsQueryBuilder}, and {@link EntityPermissionsQueryBuilder}.
 *
 * <p>None of these builders feeds a request record — paging knobs are
 * read directly off the builder by the domain client, so the assertions
 * use the builder's own getter views.
 */
class PaginationQueryBuildersTest {

    private static final int PAGE_OFFSET = 2;
    private static final int PAGE_SIZE = 25;
    private static final String SUBORDINATE_NIP = "1234567890";
    private static final String CONTEXT_NIP = "0987654321";
    private static final String CONTEXT_INTERNAL_ID = "internal-42";
    private static final String SUBUNIT_NIP = "5555555555";
    private static final String SUBUNIT_INTERNAL_ID = "subunit-9";

    @Test
    void entityRolesQueryBuilder_pageOffsetAndPageSize_setValues() {
        EntityRolesQueryBuilder builder = EntityRolesQueryBuilder.create()
                .pageOffset(PAGE_OFFSET)
                .pageSize(PAGE_SIZE);

        assertEquals(PAGE_OFFSET, builder.pageOffsetValue());
        assertEquals(PAGE_SIZE, builder.pageSizeValue());
    }

    @Test
    void subordinateEntityRolesQueryBuilder_setsAllThreeFields() {
        SubordinateEntityRolesQueryBuilder builder = SubordinateEntityRolesQueryBuilder.create()
                .subordinateEntityNip(SUBORDINATE_NIP)
                .pageOffset(PAGE_OFFSET)
                .pageSize(PAGE_SIZE);

        assertEquals(SUBORDINATE_NIP, builder.subordinateEntityNipValue());
        assertEquals(PAGE_OFFSET, builder.pageOffsetValue());
        assertEquals(PAGE_SIZE, builder.pageSizeValue());
    }

    @Test
    void subunitPermissionsQueryBuilder_subunitNipBindsTypeAndValue() {
        SubunitPermissionsQueryBuilder builder = SubunitPermissionsQueryBuilder.create()
                .subunitNip(SUBUNIT_NIP)
                .pageOffset(PAGE_OFFSET)
                .pageSize(PAGE_SIZE);

        assertEquals(SubunitPermissionsQueryBuilder.SubunitIdentifierType.NIP,
                builder.subunitIdentifierType());
        assertEquals(SUBUNIT_NIP, builder.subunitIdentifierValue());
        assertEquals(PAGE_OFFSET, builder.pageOffsetValue());
        assertEquals(PAGE_SIZE, builder.pageSizeValue());
    }

    @Test
    void subunitPermissionsQueryBuilder_subunitInternalIdBindsTypeAndValue() {
        SubunitPermissionsQueryBuilder builder = SubunitPermissionsQueryBuilder.create()
                .subunitInternalId(SUBUNIT_INTERNAL_ID);

        assertEquals(SubunitPermissionsQueryBuilder.SubunitIdentifierType.INTERNAL_ID,
                builder.subunitIdentifierType());
        assertEquals(SUBUNIT_INTERNAL_ID, builder.subunitIdentifierValue());
        assertNull(builder.pageOffsetValue());
        assertNull(builder.pageSizeValue());
    }

    @Test
    void entityPermissionsQueryBuilder_contextNipBindsTypeAndValue() {
        EntityPermissionsQueryBuilder builder = EntityPermissionsQueryBuilder.create()
                .contextNip(CONTEXT_NIP)
                .pageOffset(PAGE_OFFSET)
                .pageSize(PAGE_SIZE);

        assertEquals(EntityPermissionsQueryBuilder.ContextIdentifierType.NIP,
                builder.contextIdentifierType());
        assertEquals(CONTEXT_NIP, builder.contextIdentifierValue());
        assertEquals(PAGE_OFFSET, builder.pageOffsetValue());
        assertEquals(PAGE_SIZE, builder.pageSizeValue());
    }

    @Test
    void entityPermissionsQueryBuilder_contextInternalIdBindsTypeAndValue() {
        EntityPermissionsQueryBuilder builder = EntityPermissionsQueryBuilder.create()
                .contextInternalId(CONTEXT_INTERNAL_ID);

        assertEquals(EntityPermissionsQueryBuilder.ContextIdentifierType.INTERNAL_ID,
                builder.contextIdentifierType());
        assertEquals(CONTEXT_INTERNAL_ID, builder.contextIdentifierValue());
    }
}
