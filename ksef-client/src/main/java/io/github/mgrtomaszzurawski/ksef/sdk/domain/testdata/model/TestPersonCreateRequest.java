/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code TestDataAdmin.createPerson(...)}.
 *
 * @since 1.0.0
 */
public record TestPersonCreateRequest(
        String nip,
        String pesel,
        boolean isBailiff,
        String description,
        @Nullable Boolean isDeceased,
        @Nullable OffsetDateTime createdDate) {

    public TestPersonCreateRequest {
        Objects.requireNonNull(nip, "nip");
        Objects.requireNonNull(pesel, "pesel");
        Objects.requireNonNull(description, "description");
    }
}
