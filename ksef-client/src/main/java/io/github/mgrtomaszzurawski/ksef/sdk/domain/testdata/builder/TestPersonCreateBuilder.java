/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Builder for KSeF test person creation requests.
 * <p>
 * Required: nip, pesel, isBailiff, description.
 * Optional: isDeceased, createdDate.
 * <p>
 * Usage:
 * <pre>{@code
 * TestPersonCreateRequest request = TestPersonCreateBuilder
 *     .create("1234567890", "12345678901", false, "Test person")
 *     .build();
 * }</pre>
 */
public final class TestPersonCreateBuilder {

    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_PESEL = "pesel is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";

    private final String nip;
    private final String pesel;
    private final boolean isBailiff;
    private final String description;
    private Boolean isDeceased;
    private OffsetDateTime createdDate;

    private TestPersonCreateBuilder(String nip, String pesel, boolean isBailiff, String description) {
        this.nip = Objects.requireNonNull(nip, ERR_NULL_NIP);
        this.pesel = Objects.requireNonNull(pesel, ERR_NULL_PESEL);
        this.isBailiff = isBailiff;
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
    }

    /**
     * Create a builder with required fields.
     *
     * @param nip NIP of the taxpayer entity this person belongs to
     * @param pesel PESEL of the person
     * @param isBailiff whether the person is a bailiff
     * @param description human-readable description
     */
    public static TestPersonCreateBuilder create(String nip, String pesel, boolean isBailiff,
                                                  String description) {
        return new TestPersonCreateBuilder(nip, pesel, isBailiff, description);
    }

    /**
     * Set whether the person is deceased.
     *
     * @param isDeceased true if the person is deceased
     */
    public TestPersonCreateBuilder isDeceased(boolean isDeceased) {
        this.isDeceased = isDeceased;
        return this;
    }

    /**
     * Set the optional creation date.
     *
     * @param createdDate person creation date
     */
    public TestPersonCreateBuilder createdDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
    public TestPersonCreateBuilder toBuilder() {
        TestPersonCreateBuilder copy = new TestPersonCreateBuilder(this.nip, this.pesel, this.isBailiff, this.description);
        copy.isDeceased = this.isDeceased;
        copy.createdDate = this.createdDate;
        return copy;
    }

    /**
     * Build the person creation request.
     *
     * @return the request ready to pass to {@code TestDataClient.createPerson()}
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public TestPersonCreateRequest build() {
        return new TestPersonCreateRequest(nip, pesel, isBailiff, description, isDeceased, createdDate);
    }
}
