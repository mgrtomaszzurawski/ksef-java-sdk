/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * SDK request for {@code TestDataAdmin.createSubject(...)}.
 *
 * @since 1.0.0
 */
public record TestSubjectCreateRequest(
        String subjectNip,
        TestSubjectType subjectType,
        String description,
        List<TestSubunit> subunits,
        @Nullable OffsetDateTime createdDate) {

    public TestSubjectCreateRequest {
        Objects.requireNonNull(subjectNip, "subjectNip");
        Objects.requireNonNull(subjectType, "subjectType");
        Objects.requireNonNull(description, "description");
        subunits = (subunits == null) ? List.of() : List.copyOf(subunits);
    }
}
