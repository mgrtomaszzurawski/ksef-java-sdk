/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigInteger;
import org.jspecify.annotations.Nullable;

/**
 * Legal status of a taxpayer — the typed view of the
 * {@code Podmiot1/StatusInfoPodatnika} code ({@code TStatusInfoPodatnika}),
 * a closed set of four coded states in the FA(2)/FA(3) schema.
 *
 * @since 0.1.2
 */
public enum TaxpayerStatus {

    /** Taxpayer in liquidation ({@code StatusInfoPodatnika} = 1). */
    LIQUIDATION(1),
    /** Taxpayer undergoing restructuring proceedings ({@code StatusInfoPodatnika} = 2). */
    RESTRUCTURING(2),
    /** Taxpayer in bankruptcy ({@code StatusInfoPodatnika} = 3). */
    BANKRUPTCY(3),
    /** Inherited enterprise — "przedsiębiorstwo w spadku" ({@code StatusInfoPodatnika} = 4). */
    INHERITED_ENTERPRISE(4);

    private final int code;

    TaxpayerStatus(int code) {
        this.code = code;
    }

    /** The numeric code carried in the {@code StatusInfoPodatnika} element. */
    public int code() {
        return code;
    }

    /**
     * Maps a raw {@code StatusInfoPodatnika} code to the enum. Returns
     * {@code null} when the code is absent or outside the known 1..4 range.
     */
    public static @Nullable TaxpayerStatus fromCode(@Nullable BigInteger code) {
        if (code == null) {
            return null;
        }
        return switch (code.intValue()) {
            case 1 -> LIQUIDATION;
            case 2 -> RESTRUCTURING;
            case 3 -> BANKRUPTCY;
            case 4 -> INHERITED_ENTERPRISE;
            default -> null;
        };
    }
}
