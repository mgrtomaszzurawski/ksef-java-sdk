/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubunit;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Builder for KSeF test subject creation requests.
 * <p>Required: subjectNip, subjectType, description. Optional: subunits, createdDate.
 *
 * @since 1.0.0
 */
public final class TestSubjectCreateBuilder {

    private static final String ERR_NULL_SUBJECT_NIP = "subjectNip is required";
    private static final String ERR_NULL_SUBJECT_TYPE = "subjectType is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";
    private static final String ERR_NULL_SUBUNIT_NIP = "subunitNip is required";
    private static final String ERR_NULL_SUBUNIT_DESCRIPTION = "subunitDescription is required";

    private final String subjectNip;
    private final TestSubjectType subjectType;
    private final String description;
    private final List<TestSubunit> subunits = new ArrayList<>();
    private @Nullable OffsetDateTime createdDate;

    private TestSubjectCreateBuilder(String subjectNip, TestSubjectType subjectType, String description) {
        this.subjectNip = Objects.requireNonNull(subjectNip, ERR_NULL_SUBJECT_NIP);
        this.subjectType = Objects.requireNonNull(subjectType, ERR_NULL_SUBJECT_TYPE);
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
    }

    public static TestSubjectCreateBuilder create(String subjectNip, TestSubjectType subjectType,
                                                  String description) {
        return new TestSubjectCreateBuilder(subjectNip, subjectType, description);
    }

    public TestSubjectCreateBuilder addSubunit(String subunitNip, String subunitDescription) {
        subunits.add(new TestSubunit(
                Objects.requireNonNull(subunitNip, ERR_NULL_SUBUNIT_NIP),
                Objects.requireNonNull(subunitDescription, ERR_NULL_SUBUNIT_DESCRIPTION)));
        return this;
    }

    public TestSubjectCreateBuilder createdDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public TestSubjectCreateBuilder toBuilder() {
        TestSubjectCreateBuilder copy = new TestSubjectCreateBuilder(this.subjectNip, this.subjectType, this.description);
        copy.subunits.addAll(this.subunits);
        copy.createdDate = this.createdDate;
        return copy;
    }

    public TestSubjectCreateRequest build() {
        return new TestSubjectCreateRequest(subjectNip, subjectType, description, subunits, createdDate);
    }
}
