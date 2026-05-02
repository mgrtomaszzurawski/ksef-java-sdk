/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.common;

import io.github.mgrtomaszzurawski.ksef.client.model.StatusInfoRaw;
import java.util.List;

/**
 * Status information returned by KSeF for various operations.
 *
 * @param code numeric status code (e.g. 200 = success, 415 = session busy)
 * @param description human-readable status description
 * @param details optional additional detail messages
 */
public record StatusInfo(int code, String description, List<String> details) {

    /**
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public static StatusInfo from(StatusInfoRaw raw) {
        if (raw == null) {
            return null;
        }
        return new StatusInfo(
                raw.getCode(),
                raw.getDescription(),
                raw.getDetails() != null ? List.copyOf(raw.getDetails()) : List.of());
    }
}
