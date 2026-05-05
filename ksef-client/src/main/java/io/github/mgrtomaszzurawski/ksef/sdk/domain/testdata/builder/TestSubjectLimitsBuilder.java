/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import java.util.Objects;

/**
 * Builder for KSeF test subject certificate limits override requests.
 * <p>Required: subjectIdentifierType. Optional: maxEnrollments, maxCertificates.
 *
 * @since 1.0.0
 */
public final class TestSubjectLimitsBuilder {

    private static final String ERR_NULL_SUBJECT_IDENTIFIER_TYPE = "subjectIdentifierType is required";

    private final TestSubjectIdentifierType subjectIdentifierType;
    private Integer maxEnrollments;
    private Integer maxCertificates;

    private TestSubjectLimitsBuilder(TestSubjectIdentifierType subjectIdentifierType) {
        this.subjectIdentifierType = Objects.requireNonNull(subjectIdentifierType, ERR_NULL_SUBJECT_IDENTIFIER_TYPE);
    }

    public static TestSubjectLimitsBuilder create(TestSubjectIdentifierType subjectIdentifierType) {
        return new TestSubjectLimitsBuilder(subjectIdentifierType);
    }

    public TestSubjectLimitsBuilder maxEnrollments(int maxEnrollments) {
        this.maxEnrollments = maxEnrollments;
        return this;
    }

    public TestSubjectLimitsBuilder maxCertificates(int maxCertificates) {
        this.maxCertificates = maxCertificates;
        return this;
    }

    public TestSubjectLimitsBuilder toBuilder() {
        TestSubjectLimitsBuilder copy = new TestSubjectLimitsBuilder(this.subjectIdentifierType);
        copy.maxEnrollments = this.maxEnrollments;
        copy.maxCertificates = this.maxCertificates;
        return copy;
    }

    public TestSubjectLimitsRequest build() {
        return new TestSubjectLimitsRequest(subjectIdentifierType, maxEnrollments, maxCertificates);
    }
}
