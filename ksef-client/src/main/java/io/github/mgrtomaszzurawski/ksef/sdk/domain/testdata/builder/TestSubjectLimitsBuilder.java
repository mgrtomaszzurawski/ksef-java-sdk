/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.CertificateSubjectLimitsOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollmentSubjectLimitsOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping.TestdataMappers;
import java.util.Objects;

/**
 * Builder for KSeF test subject certificate limits override requests.
 * <p>
 * Required: subjectIdentifierType.
 * Optional: maxEnrollments, maxCertificates.
 * <p>
 * Usage:
 * <pre>{@code
 * SetSubjectLimitsRequestRaw request = TestSubjectLimitsBuilder
 *     .create(TestSubjectIdentifierType.NIP)
 *     .maxEnrollments(20)
 *     .maxCertificates(10)
 *     .build();
 * }</pre>
 */
public final class TestSubjectLimitsBuilder {

    private static final String ERR_NULL_SUBJECT_IDENTIFIER_TYPE = "subjectIdentifierType is required";

    private final TestSubjectIdentifierType subjectIdentifierType;
    private Integer maxEnrollments;
    private Integer maxCertificates;

    private TestSubjectLimitsBuilder(TestSubjectIdentifierType subjectIdentifierType) {
        this.subjectIdentifierType = Objects.requireNonNull(subjectIdentifierType, ERR_NULL_SUBJECT_IDENTIFIER_TYPE);
    }

    /**
     * Create a builder with the required subject identifier type.
     *
     * @param subjectIdentifierType type of subject identifier (Nip, Pesel, Fingerprint)
     */
    public static TestSubjectLimitsBuilder create(TestSubjectIdentifierType subjectIdentifierType) {
        return new TestSubjectLimitsBuilder(subjectIdentifierType);
    }

    /**
     * Set the max enrollments override.
     *
     * @param maxEnrollments maximum number of certificate enrollments
     */
    public TestSubjectLimitsBuilder maxEnrollments(int maxEnrollments) {
        this.maxEnrollments = maxEnrollments;
        return this;
    }

    /**
     * Set the max certificates override.
     *
     * @param maxCertificates maximum number of active certificates
     */
    public TestSubjectLimitsBuilder maxCertificates(int maxCertificates) {
        this.maxCertificates = maxCertificates;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public TestSubjectLimitsBuilder toBuilder() {
        TestSubjectLimitsBuilder copy = new TestSubjectLimitsBuilder(this.subjectIdentifierType);
        copy.maxEnrollments = this.maxEnrollments;
        copy.maxCertificates = this.maxCertificates;
        return copy;
    }

    /**
     * Build the subject limits request.
     *
     * @return the request ready to pass to {@code TestDataClient.setSubjectLimits()}
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public SetSubjectLimitsRequestRaw build() {
        SetSubjectLimitsRequestRaw request = new SetSubjectLimitsRequestRaw();
        request.setSubjectIdentifierType(TestdataMappers.toSubjectIdentifierTypeRaw(subjectIdentifierType));
        if (maxEnrollments != null) {
            EnrollmentSubjectLimitsOverrideRaw enrollment = new EnrollmentSubjectLimitsOverrideRaw();
            enrollment.setMaxEnrollments(maxEnrollments);
            request.setEnrollment(enrollment);
        }
        if (maxCertificates != null) {
            CertificateSubjectLimitsOverrideRaw certificate = new CertificateSubjectLimitsOverrideRaw();
            certificate.setMaxCertificates(maxCertificates);
            request.setCertificate(certificate);
        }
        return request;
    }
}
