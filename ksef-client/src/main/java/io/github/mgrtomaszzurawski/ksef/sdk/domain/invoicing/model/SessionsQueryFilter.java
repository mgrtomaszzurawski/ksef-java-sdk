/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Filter for {@code GET /sessions} (Codex round-9 manual-validation A.2.4).
 *
 * <p>{@code sessionType} is REQUIRED per OpenAPI ({@code GET /sessions}
 * declares it {@code required: true}). All other fields are optional;
 * {@code null} means "no filter on this axis". Use {@link #forOnline()},
 * {@link #forBatch()}, or {@link #builder(KsefSessionType)} for fluent
 * construction.
 *
 * @param sessionType narrow to ONLINE or BATCH (required)
 * @param referenceNumber narrow to one specific session reference
 * @param dateCreatedFrom inclusive lower bound on session creation time
 * @param dateCreatedTo inclusive upper bound
 * @param dateClosedFrom inclusive lower bound on session close time
 * @param dateClosedTo inclusive upper bound
 * @param dateModifiedFrom inclusive lower bound on last-status-update time
 * @param dateModifiedTo inclusive upper bound
 * @param statuses narrow to one or more numeric status codes (e.g.
 *     {@code [100, 200]} for in-flight + success)
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
        @Nullable List<Integer> statuses) {

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

    public static Builder builder(KsefSessionType sessionType) {
        return new Builder(sessionType);
    }

    public static final class Builder {
        private final KsefSessionType sessionType;
        private String referenceNumber;
        private OffsetDateTime dateCreatedFrom;
        private OffsetDateTime dateCreatedTo;
        private OffsetDateTime dateClosedFrom;
        private OffsetDateTime dateClosedTo;
        private OffsetDateTime dateModifiedFrom;
        private OffsetDateTime dateModifiedTo;
        private List<Integer> statuses;

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
        public Builder statuses(Integer... codes) { this.statuses = List.of(codes); return this; }

        public SessionsQueryFilter build() {
            return new SessionsQueryFilter(sessionType, referenceNumber,
                    dateCreatedFrom, dateCreatedTo, dateClosedFrom, dateClosedTo,
                    dateModifiedFrom, dateModifiedTo, statuses);
        }
    }
}
