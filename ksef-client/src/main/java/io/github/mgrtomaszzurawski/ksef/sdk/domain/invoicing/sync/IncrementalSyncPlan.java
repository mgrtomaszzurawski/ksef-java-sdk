/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Plan for an {@link InvoiceSyncClient#sync} call.
 *
 * <p>Built via {@link #builder()}. Default {@link #subjectTypes} is all
 * four (SUBJECT1/2/3/AUTHORIZED) per spec recommendation in
 * {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md:29}.
 * Default {@link #dateType} is {@link InvoiceQueryDateType#PERMANENT_STORAGE}.
 *
 * @param from start of the sync window (inclusive). Used only when no
 *     checkpoint is found in {@link CheckpointStore}; subsequent runs
 *     resume from the saved cursor.
 * @param to end of the sync window, or {@code null} for open-ended
 *     (recommended for continuous sync per
 *     {@code przyrostowe-pobieranie-faktur.md:91}).
 * @param subjectTypes which subject types to iterate (each gets its own checkpoint).
 * @param dateType which date type drives cursor advancement; must be
 *     {@link InvoiceQueryDateType#PERMANENT_STORAGE} for spec-conformant HWM behavior.
 * @param outputDirectory parent directory under which per-window export
 *     packages are downloaded, decrypted, and unzipped. Required.
 * @param fullContent whether to download invoice XML content
 *     ({@code true}) or only metadata ({@code false}). Default
 *     {@code true} — metadata-only sync still iterates packages but
 *     {@link InvoiceSink} receives {@code null} for {@code xmlPath}.
 */
public record IncrementalSyncPlan(
        OffsetDateTime from,
        OffsetDateTime to,
        List<InvoiceQuerySubjectType> subjectTypes,
        InvoiceQueryDateType dateType,
        Path outputDirectory,
        boolean fullContent) {

    private static final String ERR_NULL_FROM = "from must not be null";
    private static final String ERR_NULL_SUBJECTS = "subjectTypes must not be null or empty";
    private static final String ERR_NULL_DATE_TYPE = "dateType must not be null";
    private static final String ERR_NULL_OUTPUT_DIR = "outputDirectory must not be null";

    public IncrementalSyncPlan {
        Objects.requireNonNull(from, ERR_NULL_FROM);
        Objects.requireNonNull(dateType, ERR_NULL_DATE_TYPE);
        Objects.requireNonNull(outputDirectory, ERR_NULL_OUTPUT_DIR);
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
        private Path outputDirectory;
        private boolean fullContent = true;

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

        public Builder outputDirectory(Path dir) {
            this.outputDirectory = dir;
            return this;
        }

        public Builder fullContent(boolean enabled) {
            this.fullContent = enabled;
            return this;
        }

        public IncrementalSyncPlan build() {
            return new IncrementalSyncPlan(from, to, subjectTypes, dateType, outputDirectory, fullContent);
        }
    }
}
