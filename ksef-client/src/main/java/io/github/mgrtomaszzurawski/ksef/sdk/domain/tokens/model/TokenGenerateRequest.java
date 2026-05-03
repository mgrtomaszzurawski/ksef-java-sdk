/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

import java.util.List;
import java.util.Objects;

/**
 * SDK request payload for {@code TokenClient.generate(...)}. Produced by
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder#build()}.
 *
 * @param description token description (5-256 characters)
 * @param permissions non-empty list of permissions to grant the token
 */
public record TokenGenerateRequest(String description, List<TokenPermissionType> permissions) {

    private static final String ERR_DESCRIPTION_NULL = "description must not be null";
    private static final String ERR_PERMISSIONS_NULL = "permissions must not be null";

    public TokenGenerateRequest {
        Objects.requireNonNull(description, ERR_DESCRIPTION_NULL);
        Objects.requireNonNull(permissions, ERR_PERMISSIONS_NULL);
        permissions = List.copyOf(permissions);
    }
}
