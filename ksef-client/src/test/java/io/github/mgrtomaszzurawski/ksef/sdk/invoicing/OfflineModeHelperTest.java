/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session.OnlineSession;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link OnlineSession#shouldUseOfflineMode(LocalDate, LocalDate)} —
 * REQ-OFFLINE-002 helper that decides whether a normal send must use the
 * offline-mode wire flag based on calendar-date comparison.
 *
 * <p>Spec: {@code ksef-docs/offline/automatyczne-okreslanie-trybu-offline.md} —
 * "an invoice is automatically considered offline when the calendar day of
 * issueDate is earlier than the calendar day of invoicingDate".
 *
 * <p>Covers TC-SESS-006.
 */
class OfflineModeHelperTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 5, 1);

    @Test
    void shouldUseOfflineMode_whenIssueBeforeInvoicing_returnsTrue() {
        boolean result = OnlineSession.shouldUseOfflineMode(ISSUE_DATE, ISSUE_DATE.plusDays(1));

        assertTrue(result);
    }

    @Test
    void shouldUseOfflineMode_whenIssueSameDayAsInvoicing_returnsFalse() {
        boolean result = OnlineSession.shouldUseOfflineMode(ISSUE_DATE, ISSUE_DATE);

        assertFalse(result);
    }

    @Test
    void shouldUseOfflineMode_whenIssueAfterInvoicing_returnsFalse() {
        boolean result = OnlineSession.shouldUseOfflineMode(ISSUE_DATE.plusDays(1), ISSUE_DATE);

        assertFalse(result);
    }

    @Test
    void shouldUseOfflineMode_whenIssueOneSecondBeforeMidnightInvoicingNextDay_returnsTrue() {
        // Calendar-day comparison only — two LocalDates differ by one day no matter
        // how close together they were as instants.
        boolean result = OnlineSession.shouldUseOfflineMode(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2));

        assertTrue(result);
    }

    @Test
    void shouldUseOfflineMode_whenIssueDateNull_throwsNullPointer() {
        LocalDate invoicingDate = ISSUE_DATE;

        assertThrows(NullPointerException.class,
                () -> OnlineSession.shouldUseOfflineMode(null, invoicingDate));
    }

    @Test
    void shouldUseOfflineMode_whenInvoicingDateNull_throwsNullPointer() {
        LocalDate issueDate = ISSUE_DATE;

        assertThrows(NullPointerException.class,
                () -> OnlineSession.shouldUseOfflineMode(issueDate, null));
    }
}
