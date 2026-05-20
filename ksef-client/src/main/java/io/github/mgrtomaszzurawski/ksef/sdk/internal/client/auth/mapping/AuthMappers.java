/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.TokenInfoRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.model.TokenInfo;
import org.jspecify.annotations.Nullable;

/**
 * Internal mappers for auth-related types from generated {@code *Raw}
 * types to the {@code internal.client.auth.model} records. Lives in a
 * non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
 */
public final class AuthMappers {

    private AuthMappers() { }

    public static @Nullable TokenInfo toTokenInfo(@Nullable TokenInfoRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new TokenInfo(rawValue.getToken(), rawValue.getValidUntil());
    }
}
