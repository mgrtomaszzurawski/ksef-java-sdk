/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@code PermissionQueryPaging.validate} fail-fast wired into the
 * compact constructors of all 8 permission query request records.
 * KSeF enforces {@code pageSize ∈ [10, 100]} on every permissions query
 * endpoint and rejects out-of-range with 21405; the SDK rejects the
 * same range at {@code build()} time so consumers see the offending
 * value next to the call site.
 */
class PermissionQueryPagingValidationTest {

    private static final int MIN_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int ABOVE_MAX = MAX_PAGE_SIZE + 1;
    private static final int BELOW_MIN = MIN_PAGE_SIZE - 1;

    @Test
    void personalPermissions_rejectsPageSizeAboveMax() {
        var builder = PersonalPermissionsQueryBuilder.create().pageSize(ABOVE_MAX);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(thrown.getMessage().contains(Integer.toString(ABOVE_MAX)),
                () -> "Error must reference the offending value: " + thrown.getMessage());
    }

    @Test
    void personalPermissions_rejectsPageSizeBelowMin() {
        var builder = PersonalPermissionsQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void personPermissions_rejectsPageSizeAboveMax() {
        var builder = PersonPermissionsQueryBuilder
                .permissionsGrantedInCurrentContext().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void personPermissions_rejectsPageSizeBelowMin() {
        var builder = PersonPermissionsQueryBuilder
                .permissionsGrantedInCurrentContext().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void subunitPermissions_rejectsPageSizeAboveMax() {
        var builder = SubunitPermissionsQueryBuilder.create().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void subunitPermissions_rejectsPageSizeBelowMin() {
        var builder = SubunitPermissionsQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityPermissions_rejectsPageSizeAboveMax() {
        var builder = EntityPermissionsQueryBuilder.create().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityPermissions_rejectsPageSizeBelowMin() {
        var builder = EntityPermissionsQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityRoles_rejectsPageSizeAboveMax() {
        var builder = EntityRolesQueryBuilder.create().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityRoles_rejectsPageSizeBelowMin() {
        var builder = EntityRolesQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void subordinateRoles_rejectsPageSizeAboveMax() {
        var builder = SubordinateEntityRolesQueryBuilder.create().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void subordinateRoles_rejectsPageSizeBelowMin() {
        var builder = SubordinateEntityRolesQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityAuthorizations_rejectsPageSizeAboveMax() {
        var builder = EntityAuthorizationPermissionsQueryBuilder.granted().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void entityAuthorizations_rejectsPageSizeBelowMin() {
        var builder = EntityAuthorizationPermissionsQueryBuilder.granted().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void euEntities_rejectsPageSizeAboveMax() {
        var builder = EuEntityPermissionsQueryBuilder.create().pageSize(ABOVE_MAX);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void euEntities_rejectsPageSizeBelowMin() {
        var builder = EuEntityPermissionsQueryBuilder.create().pageSize(BELOW_MIN);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void anyRecord_acceptsBoundaryPageSizes() {
        // Spot-check boundary values (10 and 100) flow through every record cleanly.
        assertEquals(MIN_PAGE_SIZE, PersonalPermissionsQueryBuilder.create()
                .pageSize(MIN_PAGE_SIZE).build().pageSize());
        assertEquals(MAX_PAGE_SIZE, PersonalPermissionsQueryBuilder.create()
                .pageSize(MAX_PAGE_SIZE).build().pageSize());
        assertEquals(MAX_PAGE_SIZE, EuEntityPermissionsQueryBuilder.create()
                .pageSize(MAX_PAGE_SIZE).build().pageSize());
        assertEquals(MAX_PAGE_SIZE, EntityAuthorizationPermissionsQueryBuilder.granted()
                .pageSize(MAX_PAGE_SIZE).build().pageSize());
    }

    @Test
    void anyRecord_rejectsNegativePageOffset() {
        var builder = PersonalPermissionsQueryBuilder.create().pageOffset(-1);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(thrown.getMessage().contains("-1"));
    }
}
