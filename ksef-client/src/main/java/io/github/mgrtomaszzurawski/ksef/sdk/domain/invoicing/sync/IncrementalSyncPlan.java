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
import org.jspecify.annotations.Nullable;

/**
 * Plan for an
 * {@code Invoices.syncAsStream(plan, checkpointStore)} call.
 *
 * <p>Built via {@link #builder()}. Default {@link #subjectTypes} is all
 * four (SUBJECT1/2/3/AUTHORIZED) per spec recommendation in
 * {@code ksef-docs/pobieranie-faktur/przyrostowe-pobieranie-faktur.md:29}.
 *
 * <p>Codex round-9 manual-validation A.1.3 — the date type for incremental
 * sync is fixed to {@link InvoiceQueryDateType#PERMANENT_STORAGE}. The spec
 * ("Kluczowe znaczenie daty PermanentStorage") states HWM only works with
 * PermanentStorage; allowing other values would silently break the
 * commit-after-accept invariant. Earlier builds exposed a {@code dateType()}
 * setter on the builder; that knob has been removed.
 *
 * @param from start of the sync window (inclusive). Used only when no
 *     checkpoint is found in {@link CheckpointStore}; subsequent runs
 *     resume from the saved cursor.
 * @param to end of the sync window, or {@code null} for open-ended
 *     (recommended for continuous sync per
 *     {@code przyrostowe-pobieranie-faktur.md:91}).
 * @param subjectTypes which subject types to iterate (each gets its own checkpoint).
 * @param outputDirectory parent directory under which per-window export
 *     packages are downloaded, decrypted, and unzipped. Required.
 * @param fullContent whether to download invoice XML content
 *     ({@code true}) or only metadata ({@code false}). Default
 *     {@code true} — metadata-only sync still iterates packages but
 *     {@link InvoiceSink} receives {@code null} for {@code xmlPath}.
 *
 * @since 1.0.0
 */
public record IncrementalSyncPlan(
        OffsetDateTime from,
        @Nullable OffsetDateTime to,
        List<InvoiceQuerySubjectType> subjectTypes,
        Path outputDirectory,
        boolean fullContent) {

    /**
     * Fixed date-type for incremental sync — see class-level Javadoc.
     */
    public static final InvoiceQueryDateType DATE_TYPE = InvoiceQueryDateType.PERMANENT_STORAGE;

    private static final String ERR_NULL_FROM = "from must not be null";
    private static final String ERR_NULL_SUBJECTS = "subjectTypes must not be null or empty";
    private static final String ERR_NULL_OUTPUT_DIR = "outputDirectory must not be null";

    public IncrementalSyncPlan {
        Objects.requireNonNull(from, ERR_NULL_FROM);
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

        private @Nullable OffsetDateTime from;
        private @Nullable OffsetDateTime to;
        private List<InvoiceQuerySubjectType> subjectTypes = List.of(
                InvoiceQuerySubjectType.SUBJECT1,
                InvoiceQuerySubjectType.SUBJECT2,
                InvoiceQuerySubjectType.SUBJECT3,
                InvoiceQuerySubjectType.SUBJECT_AUTHORIZED);
        private @Nullable Path outputDirectory;
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

        public Builder outputDirectory(Path dir) {
            this.outputDirectory = dir;
            return this;
        }

        public Builder fullContent(boolean enabled) {
            this.fullContent = enabled;
            return this;
        }

        public IncrementalSyncPlan build() {
            return new IncrementalSyncPlan(
                    Objects.requireNonNull(from, ERR_NULL_FROM),
                    to,
                    subjectTypes,
                    Objects.requireNonNull(outputDirectory, ERR_NULL_OUTPUT_DIR),
                    fullContent);
        }
    }
}
