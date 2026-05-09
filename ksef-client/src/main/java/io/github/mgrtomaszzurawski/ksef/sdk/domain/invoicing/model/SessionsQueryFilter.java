/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Filter for {@code GET /sessions}.
 *
 * <p>{@code sessionType} is REQUIRED per OpenAPI ({@code GET /sessions}
 * declares it {@code required: true}). All other fields are optional;
 * {@code null} means "no filter on this axis". Use {@link #forOnline()}
 * or {@link #forBatch()} for fluent construction.
 *
 * @param sessionType narrow to ONLINE or BATCH (required)
 * @param referenceNumber narrow to one specific session reference
 * @param dateCreatedFrom inclusive lower bound on session creation time
 * @param dateCreatedTo inclusive upper bound
 * @param dateClosedFrom inclusive lower bound on session close time
 * @param dateClosedTo inclusive upper bound
 * @param dateModifiedFrom inclusive lower bound on last-status-update time
 * @param dateModifiedTo inclusive upper bound
 * @param statuses narrow to one or more {@link CommonSessionStatus} values
 *     (e.g. {@code [InProgress, Failed]} for in-flight + failed sessions);
 *     wire format is the string enum name per OpenAPI {@code CommonSessionStatus}
 *
 * @since 1.0.0
 */
public record SessionsQueryFilter(
        KsefSessionType sessionType,
        @Nullable String referenceNumber,
        @Nullable OffsetDateTime dateCreatedFrom,
        @Nullable OffsetDateTime dateCreatedTo,
        @Nullable OffsetDateTime dateClosedFrom,
        @Nullable OffsetDateTime dateClosedTo,
        @Nullable OffsetDateTime dateModifiedFrom,
        @Nullable OffsetDateTime dateModifiedTo,
        @Nullable List<CommonSessionStatus> statuses) {

    private static final String ERR_SESSION_TYPE_NULL =
            "sessionType is required by GET /sessions (OpenAPI required:true)";

    public SessionsQueryFilter {
        java.util.Objects.requireNonNull(sessionType, ERR_SESSION_TYPE_NULL);
        statuses = statuses == null ? null : List.copyOf(statuses);
    }

    /** Convenience: filter restricted to {@link KsefSessionType#ONLINE}. */
    public static Builder forOnline() {
        return new Builder(KsefSessionType.ONLINE);
    }

    /** Convenience: filter restricted to {@link KsefSessionType#BATCH}. */
    public static Builder forBatch() {
        return new Builder(KsefSessionType.BATCH);
    }

    public static final class Builder {
        private final KsefSessionType sessionType;
        private @Nullable String referenceNumber;
        private @Nullable OffsetDateTime dateCreatedFrom;
        private @Nullable OffsetDateTime dateCreatedTo;
        private @Nullable OffsetDateTime dateClosedFrom;
        private @Nullable OffsetDateTime dateClosedTo;
        private @Nullable OffsetDateTime dateModifiedFrom;
        private @Nullable OffsetDateTime dateModifiedTo;
        private @Nullable List<CommonSessionStatus> statuses;

        private Builder(KsefSessionType sessionType) {
            this.sessionType = java.util.Objects.requireNonNull(sessionType, ERR_SESSION_TYPE_NULL);
        }

        public Builder referenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; return this; }
        public Builder dateCreatedFrom(OffsetDateTime dateCreatedFrom) { this.dateCreatedFrom = dateCreatedFrom; return this; }
        public Builder dateCreatedTo(OffsetDateTime dateCreatedTo) { this.dateCreatedTo = dateCreatedTo; return this; }
        public Builder dateClosedFrom(OffsetDateTime dateClosedFrom) { this.dateClosedFrom = dateClosedFrom; return this; }
        public Builder dateClosedTo(OffsetDateTime dateClosedTo) { this.dateClosedTo = dateClosedTo; return this; }
        public Builder dateModifiedFrom(OffsetDateTime dateModifiedFrom) { this.dateModifiedFrom = dateModifiedFrom; return this; }
        public Builder dateModifiedTo(OffsetDateTime dateModifiedTo) { this.dateModifiedTo = dateModifiedTo; return this; }
        public Builder statuses(CommonSessionStatus... values) { this.statuses = List.of(values); return this; }

        public SessionsQueryFilter build() {
            return new SessionsQueryFilter(sessionType, referenceNumber,
                    dateCreatedFrom, dateCreatedTo, dateClosedFrom, dateClosedTo,
                    dateModifiedFrom, dateModifiedTo, statuses);
        }
    }
}
