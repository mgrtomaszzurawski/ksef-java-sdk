/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping;

import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateUsageRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.StatusInfoRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.core.StatusInfo;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Internal mappers from generated {@code *Raw} types to public common
 * domain records. Lives in a non-exported package; consumers can't reach it.
 *
 * @since 1.0.0
 */
public final class CommonMappers {

    private CommonMappers() { }

    public static PublicKeyCertificate toPublicKeyCertificate(PublicKeyCertificateRaw rawValue) {
        List<PublicKeyCertificateUsage> mappedUsage = rawValue.getUsage().stream().map(CommonMappers::toPublicKeyCertificateUsage).toList();
        return PublicKeyCertificate.from(
                rawValue.getCertificate(),
                rawValue.getValidFrom(),
                rawValue.getValidTo(),
                mappedUsage);
    }

    public static @Nullable PublicKeyCertificateUsage toPublicKeyCertificateUsage(@Nullable PublicKeyCertificateUsageRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return switch (rawValue) {
            case KSEF_TOKEN_ENCRYPTION -> PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION;
            case SYMMETRIC_KEY_ENCRYPTION -> PublicKeyCertificateUsage.SYMMETRIC_KEY_ENCRYPTION;
        };
    }

    public static @Nullable StatusInfo toStatusInfo(@Nullable StatusInfoRaw rawValue) {
        if (rawValue == null) {
            return null;
        }
        return new StatusInfo(
                rawValue.getCode(),
                rawValue.getDescription(),
                rawValue.getDetails() != null ? List.copyOf(rawValue.getDetails()) : List.of());
    }

}
