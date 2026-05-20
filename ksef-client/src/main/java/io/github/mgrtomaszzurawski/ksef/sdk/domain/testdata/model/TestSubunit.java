/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * Subunit of a test subject (used in {@link TestSubjectCreateRequest}).
 *
 * @since 0.1.0
 */
public record TestSubunit(String subjectNip, String description) {

    public TestSubunit {
        Objects.requireNonNull(subjectNip, "subjectNip");
        Objects.requireNonNull(description, "description");
    }
}
