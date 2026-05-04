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
 * <p>All fields are optional; {@code null} means "no filter on this axis".
 * Use {@link #builder()} for a fluent construction.
 *
 * @param sessionType narrow to ONLINE or BATCH
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
        @Nullable KsefSessionType sessionType,
        @Nullable String referenceNumber,
        @Nullable OffsetDateTime dateCreatedFrom,
        @Nullable OffsetDateTime dateCreatedTo,
        @Nullable OffsetDateTime dateClosedFrom,
        @Nullable OffsetDateTime dateClosedTo,
        @Nullable OffsetDateTime dateModifiedFrom,
        @Nullable OffsetDateTime dateModifiedTo,
        @Nullable List<Integer> statuses) {

    public SessionsQueryFilter {
        statuses = statuses == null ? null : List.copyOf(statuses);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private KsefSessionType sessionType;
        private String referenceNumber;
        private OffsetDateTime dateCreatedFrom;
        private OffsetDateTime dateCreatedTo;
        private OffsetDateTime dateClosedFrom;
        private OffsetDateTime dateClosedTo;
        private OffsetDateTime dateModifiedFrom;
        private OffsetDateTime dateModifiedTo;
        private List<Integer> statuses;

        private Builder() { }

        public Builder sessionType(KsefSessionType type) { this.sessionType = type; return this; }
        public Builder referenceNumber(String ref) { this.referenceNumber = ref; return this; }
        public Builder dateCreatedFrom(OffsetDateTime from) { this.dateCreatedFrom = from; return this; }
        public Builder dateCreatedTo(OffsetDateTime to) { this.dateCreatedTo = to; return this; }
        public Builder dateClosedFrom(OffsetDateTime from) { this.dateClosedFrom = from; return this; }
        public Builder dateClosedTo(OffsetDateTime to) { this.dateClosedTo = to; return this; }
        public Builder dateModifiedFrom(OffsetDateTime from) { this.dateModifiedFrom = from; return this; }
        public Builder dateModifiedTo(OffsetDateTime to) { this.dateModifiedTo = to; return this; }
        public Builder statuses(Integer... codes) { this.statuses = List.of(codes); return this; }

        public SessionsQueryFilter build() {
            return new SessionsQueryFilter(sessionType, referenceNumber,
                    dateCreatedFrom, dateCreatedTo, dateClosedFrom, dateClosedTo,
                    dateModifiedFrom, dateModifiedTo, statuses);
        }
    }
}
