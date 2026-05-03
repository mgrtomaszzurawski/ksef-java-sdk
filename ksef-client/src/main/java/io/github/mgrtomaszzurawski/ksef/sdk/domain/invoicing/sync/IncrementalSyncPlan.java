/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Plan for an {@link InvoiceSyncClient#sync} call.
 *
 * <p>Built via {@link #builder()}. Defaults:
 *
 * <ul>
 *   <li>{@link #subjectTypes} = all four (SUBJECT1/2/3/AUTHORIZED) per
 *       spec recommendation in
 *       {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md:29}</li>
 *   <li>{@link #dateType} = {@link InvoiceQueryDateType#PERMANENT_STORAGE}</li>
 * </ul>
 *
 * @param from start of the sync window (inclusive). Used only on the
 *     first run; subsequent runs resume from {@link CheckpointStore}
 * @param to end of the sync window, or {@code null} for open-ended (recommended for continuous sync)
 * @param subjectTypes which subject types to iterate (independently — each gets its own checkpoint)
 * @param dateType which date type drives the cursor — must be
 *     {@link InvoiceQueryDateType#PERMANENT_STORAGE} for spec-conformant HWM behavior
 */
public record IncrementalSyncPlan(
        OffsetDateTime from,
        OffsetDateTime to,
        List<InvoiceQuerySubjectType> subjectTypes,
        InvoiceQueryDateType dateType) {

    private static final String ERR_NULL_FROM = "from must not be null";
    private static final String ERR_NULL_SUBJECTS = "subjectTypes must not be null or empty";
    private static final String ERR_NULL_DATE_TYPE = "dateType must not be null";

    public IncrementalSyncPlan {
        Objects.requireNonNull(from, ERR_NULL_FROM);
        Objects.requireNonNull(dateType, ERR_NULL_DATE_TYPE);
        if (subjectTypes == null || subjectTypes.isEmpty()) {
            throw new IllegalArgumentException(ERR_NULL_SUBJECTS);
        }
        subjectTypes = List.copyOf(subjectTypes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private OffsetDateTime from;
        private OffsetDateTime to;
        private List<InvoiceQuerySubjectType> subjectTypes = List.of(
                InvoiceQuerySubjectType.SUBJECT1,
                InvoiceQuerySubjectType.SUBJECT2,
                InvoiceQuerySubjectType.SUBJECT3,
                InvoiceQuerySubjectType.SUBJECT_AUTHORIZED);
        private InvoiceQueryDateType dateType = InvoiceQueryDateType.PERMANENT_STORAGE;

        private Builder() { }

        public Builder from(OffsetDateTime from) {
            this.from = from;
            return this;
        }

        public Builder to(OffsetDateTime to) {
            this.to = to;
            return this;
        }

        public Builder subjectTypes(InvoiceQuerySubjectType... types) {
            this.subjectTypes = List.of(types);
            return this;
        }

        public Builder dateType(InvoiceQueryDateType type) {
            this.dateType = type;
            return this;
        }

        public IncrementalSyncPlan build() {
            return new IncrementalSyncPlan(from, to, subjectTypes, dateType);
        }
    }
}
