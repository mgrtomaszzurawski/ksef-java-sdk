/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectCreateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for KSeF test subject creation requests.
 * <p>
 * Required: subjectNip, subjectType, description.
 * Optional: subunits, createdDate.
 * <p>
 * Usage:
 * <pre>{@code
 * SubjectCreateRequestRaw request = TestSubjectCreateBuilder
 *     .create("1234567890", TestSubjectType.JST, "Test taxpayer")
 *     .build();
 * }</pre>
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
    private final List<SubunitRaw> subunits = new ArrayList<>();
    private OffsetDateTime createdDate;

    private TestSubjectCreateBuilder(String subjectNip, TestSubjectType subjectType, String description) {
        this.subjectNip = Objects.requireNonNull(subjectNip, ERR_NULL_SUBJECT_NIP);
        this.subjectType = Objects.requireNonNull(subjectType, ERR_NULL_SUBJECT_TYPE);
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
    }

    /**
     * Create a builder with required fields.
     *
     * @param subjectNip NIP of the test subject
     * @param subjectType type of the subject (EnforcementAuthority, VatGroup, JST)
     * @param description human-readable description
     */
    public static TestSubjectCreateBuilder create(String subjectNip, TestSubjectType subjectType,
                                                   String description) {
        return new TestSubjectCreateBuilder(subjectNip, subjectType, description);
    }

    /**
     * Add a subunit to the subject.
     *
     * @param subunitNip NIP of the subunit
     * @param subunitDescription description of the subunit
     */
    public TestSubjectCreateBuilder addSubunit(String subunitNip, String subunitDescription) {
        SubunitRaw subunit = new SubunitRaw();
        subunit.setSubjectNip(Objects.requireNonNull(subunitNip, ERR_NULL_SUBUNIT_NIP));
        subunit.setDescription(Objects.requireNonNull(subunitDescription, ERR_NULL_SUBUNIT_DESCRIPTION));
        subunits.add(subunit);
        return this;
    }

    /**
     * Set the optional creation date.
     *
     * @param createdDate subject creation date
     */
    public TestSubjectCreateBuilder createdDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public TestSubjectCreateBuilder toBuilder() {
        TestSubjectCreateBuilder copy = new TestSubjectCreateBuilder(this.subjectNip, this.subjectType, this.description);
        copy.subunits.addAll(this.subunits);
        copy.createdDate = this.createdDate;
        return copy;
    }

    /**
     * Build the subject creation request.
     *
     * @return the request ready to pass to {@code TestDataClient.createSubject()}
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public SubjectCreateRequestRaw build() {
        SubjectCreateRequestRaw request = new SubjectCreateRequestRaw();
        request.setSubjectNip(subjectNip);
        request.setSubjectType(subjectType.toRaw());
        request.setDescription(description);
        if (!subunits.isEmpty()) {
            request.setSubunits(new ArrayList<>(subunits));
        }
        if (createdDate != null) {
            request.setCreatedDate(createdDate);
        }
        return request;
    }
}
