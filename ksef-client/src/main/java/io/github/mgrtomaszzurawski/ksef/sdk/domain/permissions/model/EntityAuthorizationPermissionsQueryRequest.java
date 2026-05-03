/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code PermissionClient.queryAuthorizations(...)}.
 * <p>{@code authorizingType} is implicitly NIP-only (KSeF spec). The
 * {@code authorizedType} accepts {@link EntityAuthorizationIdentifierType}
 * (NIP or PEPPOL_ID).
 */
public record EntityAuthorizationPermissionsQueryRequest(
        AuthorizationQueryType queryType,
        @Nullable String authorizingNip,
        @Nullable EntityAuthorizationIdentifierType authorizedType,
        @Nullable String authorizedValue,
        List<EntityAuthorizationPermissionType> permissionTypes) {

    public EntityAuthorizationPermissionsQueryRequest {
        Objects.requireNonNull(queryType, "queryType");
        permissionTypes = permissionTypes == null ? List.of() : List.copyOf(permissionTypes);
    }
}
