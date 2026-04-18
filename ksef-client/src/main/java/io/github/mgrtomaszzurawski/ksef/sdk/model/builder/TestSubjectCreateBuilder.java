/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.SubjectCreateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubunitRaw;

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
 *     .create("1234567890", SubjectTypeRaw.JST, "Test taxpayer")
 *     .build();
 * }</pre>
 */
public final class TestSubjectCreateBuilder {

    private final String subjectNip;
    private final SubjectTypeRaw subjectType;
    private final String description;
    private final List<SubunitRaw> subunits = new ArrayList<>();
    private OffsetDateTime createdDate;

    private TestSubjectCreateBuilder(String subjectNip, SubjectTypeRaw subjectType, String description) {
        this.subjectNip = Objects.requireNonNull(subjectNip, "subjectNip is required");
        this.subjectType = Objects.requireNonNull(subjectType, "subjectType is required");
        this.description = Objects.requireNonNull(description, "description is required");
    }

    /**
     * Create a builder with required fields.
     *
     * @param subjectNip NIP of the test subject
     * @param subjectType type of the subject (EnforcementAuthority, VatGroup, JST)
     * @param description human-readable description
     */
    public static TestSubjectCreateBuilder create(String subjectNip, SubjectTypeRaw subjectType,
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
        subunit.setSubjectNip(Objects.requireNonNull(subunitNip, "subunitNip is required"));
        subunit.setDescription(Objects.requireNonNull(subunitDescription, "subunitDescription is required"));
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
     * Build the subject creation request.
     *
     * @return the request ready to pass to {@code TestDataClient.createSubject()}
     */
    public SubjectCreateRequestRaw build() {
        SubjectCreateRequestRaw request = new SubjectCreateRequestRaw();
        request.setSubjectNip(subjectNip);
        request.setSubjectType(subjectType);
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
