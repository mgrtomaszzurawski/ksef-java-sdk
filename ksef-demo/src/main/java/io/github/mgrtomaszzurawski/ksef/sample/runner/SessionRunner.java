/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceLineItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceParty;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for session operations using the OnlineSession API.
 * Tests each operation separately with per-operation reporting.
 * FULL mode only.
 *
 * <p>Operations tested: openSession, send, invoiceStatus, status,
 * invoices, failedInvoices, close (with 415 retry handled by SDK),
 * UPO retrieval (by invoice ref and by KSeF number), and a
 * negative-path stale-session-recovery probe.
 */
public final class SessionRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionRunner.class);

    private static final String NAME = "session";
    private static final String OP_OPEN_SESSION = "openSession";
    private static final String OP_SEND_INVOICE = "sendInvoice";
    private static final String OP_GET_INVOICE_STATUS = "getInvoiceStatus";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_GET_INVOICES = "getInvoices";
    private static final String OP_GET_FAILED = "getFailedInvoices";
    private static final String OP_CLOSE = "close";
    private static final String OP_UPO = "getUpo";
    private static final String OP_STALE_SESSION_RECOVERY = "staleSessionRecovery";
    private static final String OP_CLEARED_BY_SUBMITTED = "getClearedBySubmittedInvoice";

    private static final String SKIP_NOT_FULL_MODE =
            "AUTH_SAFE mode — cleared(SubmittedInvoice) needs a real send first";
    private static final String OK_CONCURRENT_PERMITTED = "server permitted concurrent open";
    private static final String OK_CONCURRENT_REJECTED_PREFIX = "server rejected: ";
    private static final String DEMO_INVOICE_NUMBER_PREFIX = "DEMO/SESSION/";
    private static final String DEMO_LOCALITY = "Warszawa";
    private static final String DEMO_STREET = "Marszalkowska";
    private static final String DEMO_HOUSE = "10";
    private static final String DEMO_POSTAL = "00-001";
    private static final String DEMO_BUYER_LOCALITY = "Krakow";
    private static final String DEMO_BUYER_HOUSE = "5";
    private static final String DEMO_BUYER_POSTAL = "00-002";
    private static final String DEMO_BUYER_NIP = "9876543210";
    private static final String DEMO_LINE_DESCRIPTION = "Demo service";
    private static final String DEMO_LINE_UNIT = "szt.";
    private static final String DEMO_LINE_VAT = "23";
    private static final BigDecimal DEMO_LINE_NET = new BigDecimal("100.00");
    private static final BigDecimal DEMO_LINE_GROSS = new BigDecimal("123.00");
    private static final String DEMO_SELLER_NAME = "Demo Seller sp. z o.o.";
    private static final String DEMO_BUYER_NAME = "Demo Buyer sp. z o.o.";
    /**
     * KSeF terminal status code on a session that ends without any invoice having
     * been sent (or, for batch, without any part being uploaded). Documented in
     * ksef-docs/api-changelog.md under the "Cancelled" status: "Sesja anulowana.
     * Został przekroczony czas na wysyłkę w sesji wsadowej, lub nie przesłano
     * żadnych faktur w sesji interaktywnej." AUTH_SAFE mode opens but never sends,
     * so this is the expected close outcome there.
     */
    private static final int TERMINAL_CANCELLED_NO_INVOICES = 440;
    private static final String OK_CANCELLED_NO_INVOICES =
            "session cancelled by server (status 440) — no invoices sent in AUTH_SAFE mode (per ksef-docs)";

    private static final String REF_PREFIX = "ref=";
    private static final String INVOICES_LABEL = " invoices";
    private static final String FAILED_LABEL = " failed";
    private static final String BYTES_LABEL = " bytes";
    private static final String INVOICE_STATUS_LABEL = "invoice status";
    private static final String FAILED_INVOICE_LABEL = "failed invoice";
    private static final String NO_STATUS_PLACEHOLDER = "<no status>";
    private static final String NULL_LITERAL = "null";

    private static final String LOG_FIRST_SESSION_OPENED =
            "[{}] first session opened ref={}, probing concurrent-open behavior";
    private static final String LOG_CONCURRENT_PERMITTED = "[{}] server permitted concurrent open: ref={}";
    private static final String LOG_CONCURRENT_REJECTED = "[{}] server rejected concurrent open: {}";
    private static final String LOG_OPENED = "[{}] opened session ref={}";
    private static final String LOG_SENT_INVOICE = "[{}] sent invoice ref={}";
    private static final String LOG_INVOICE_STATUS_NULL = "[{}] {}: {}";
    private static final String LOG_INVOICE_STATUS = "[{}] {}: code={} description={} ksefNumber={}";
    private static final String LOG_INVOICE_STATUS_DETAIL = "[{}]   detail: {}";
    private static final String LOG_SESSION_STATUS = "[{}] session status: code={}";
    private static final String LOG_SESSION_INVOICES = "[{}] session invoices: {} found";
    private static final String LOG_FAILED_INVOICES = "[{}] failed invoices: {} found";
    private static final String LOG_SESSION_CLOSED = "[{}] session closed (415 retry + polling handled by SDK)";
    private static final String LOG_UPO_RETRIEVED = "[{}] UPO retrieved, size={} bytes";
    private static final String LOG_UPO_BY_KSEF_RETRIEVED = "[{}] UPO by KSeF number retrieved, size={} bytes";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        boolean fullMode = context.mode() == DemoMode.FULL;

        runStaleSessionRecovery(context, results);

        OnlineSession session = runOpenSession(context, results);
        if (session == null) {
            return results;
        }

        try (session) {
            SubmittedInvoice<Fa3Invoice> submitted = null;
            if (fullMode) {
                submitted = runSendInvoice(context, session, results);
                if (submitted != null) {
                    runGetInvoiceStatus(session, submitted.referenceNumber(), results);
                }
            }
            runGetStatus(session, results);
            runGetInvoices(session, results);
            runGetFailedInvoices(session, results);
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession closed = runArchive(context, session, results);
            if (fullMode && closed != null && submitted != null) {
                runGetCleared(closed, submitted.referenceNumber(), results);
                runGetClearedBySubmittedInvoice(closed, submitted, results);
            } else if (closed != null) {
                results.add(RunResult.skip(NAME, OP_CLEARED_BY_SUBMITTED, SKIP_NOT_FULL_MODE));
            }
        }
        return results;
    }

    /**
     * Probes the {@code closed.cleared(SubmittedInvoice)} typed overload
     * (PR15) against the SubmittedInvoice handle produced by
     * {@code session.sendInvoice(Invoice)} earlier in the run.
     */
    private <I extends io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice>
            void runGetClearedBySubmittedInvoice(
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession closed,
            SubmittedInvoice<I> submitted,
            List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var cleared = closed.cleared(submitted);
            results.add(RunResult.ok(NAME, OP_CLEARED_BY_SUBMITTED, elapsed(start),
                    REF_PREFIX + cleared.submitted().referenceNumber()));
        } catch (io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException exception) {
            results.add(RunResult.fail(NAME, OP_CLEARED_BY_SUBMITTED, elapsed(start),
                    errorMessage(exception)));
        }
    }

    /**
     * Records whether the server permits a second concurrent open for the same
     * NIP. Both outcomes are valid observations. Both probe sessions are
     * closed in {@code finally} before the happy-path test runs.
     */
    private void runStaleSessionRecovery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        OnlineSession firstSession = null;
        try {
            firstSession = context.client().invoices().sessions().online(FormCode.FA3);
            String firstRef = firstSession.referenceNumber();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_FIRST_SESSION_OPENED, NAME, firstRef);
            }
            attemptConcurrentSession(context, results, start);
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                    errorMessage(exception)));
        } finally {
            closeNoInvoicesSession(firstSession);
        }
    }

    /**
     * Manually close a stale-recovery session, swallowing the documented
     * KSeF terminal-status-440 ("Sesja anulowana") that fires when an
     * interactive session ends without any invoice having been sent. The
     * try-with-resources path would treat 440 as a fatal close-time
     * exception even though the recovery probe itself already succeeded.
     * Anything other than 440 is logged but not rethrown — throwing from
     * this finally would mask the recovery probe's own result.
     */
    private static void closeNoInvoicesSession(OnlineSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException terminalFailure) {
            if (terminalFailure.code() != TERMINAL_CANCELLED_NO_INVOICES
                    && LOGGER.isWarnEnabled()) {
                LOGGER.warn("[{}] unexpected terminal status on close: code={} description={}",
                        NAME, terminalFailure.code(), terminalFailure.description());
            }
        }
    }

    private void attemptConcurrentSession(DemoContext context, List<RunResult> results, long start) {
        OnlineSession second = null;
        try {
            second = context.client().invoices().sessions().online(FormCode.FA3);
            String secondRef = second.referenceNumber();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_CONCURRENT_PERMITTED, NAME, secondRef);
            }
            results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                    OK_CONCURRENT_PERMITTED));
        } catch (Exception rejected) {
            String rejectedClass = rejected.getClass().getSimpleName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_CONCURRENT_REJECTED, NAME, rejectedClass);
            }
            results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                    OK_CONCURRENT_REJECTED_PREFIX + rejectedClass));
        } finally {
            closeNoInvoicesSession(second);
        }
    }

    /**
     * Opens a KSeF session and transfers ownership to the caller.
     *
     * <p>The returned session must be closed by the caller. The orchestrating
     * {@link #run(DemoContext)} method wraps the returned session in
     * try-with-resources before any subsequent operation is invoked on it.
     */
    @SuppressWarnings("java:S2095")
    private OnlineSession runOpenSession(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OnlineSession session = context.client().invoices().sessions().online(FormCode.FA3);
            String sessionRef = session.referenceNumber();
            LOGGER.info(LOG_OPENED, NAME, sessionRef);
            context.state().setSessionReferenceNumber(sessionRef);
            results.add(RunResult.ok(NAME, OP_OPEN_SESSION, elapsed(start),
                    REF_PREFIX + sessionRef));
            return session;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_SESSION, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private SubmittedInvoice<Fa3Invoice> runSendInvoice(DemoContext context, OnlineSession session,
                                            List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            Fa3Invoice invoice = buildDemoInvoice(context);
            var submitted = session.sendInvoice(invoice);
            String invoiceRef = submitted.referenceNumber();
            LOGGER.info(LOG_SENT_INVOICE, NAME, invoiceRef);
            results.add(RunResult.ok(NAME, OP_SEND_INVOICE, elapsed(start),
                    REF_PREFIX + invoiceRef));
            return submitted;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SEND_INVOICE, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private static Fa3Invoice buildDemoInvoice(DemoContext context) {
        String sellerNip = context.nipIdentifier();
        return Fa3Invoice.builder()
                .invoiceNumber(DEMO_INVOICE_NUMBER_PREFIX + System.currentTimeMillis())
                .issueDate(LocalDate.now())
                .issueLocality(DEMO_LOCALITY)
                .seller(new InvoiceParty(sellerNip, DEMO_SELLER_NAME, DEMO_POSTAL,
                        DEMO_LOCALITY, DEMO_STREET, DEMO_HOUSE, null))
                .buyer(new InvoiceParty(DEMO_BUYER_NIP, DEMO_BUYER_NAME, DEMO_BUYER_POSTAL,
                        DEMO_BUYER_LOCALITY, null, DEMO_BUYER_HOUSE, null))
                .totalGrossAmount(DEMO_LINE_GROSS)
                .addLineItem(new InvoiceLineItem(1, DEMO_LINE_DESCRIPTION, DEMO_LINE_UNIT,
                        BigDecimal.ONE, DEMO_LINE_NET, DEMO_LINE_NET, DEMO_LINE_VAT))
                .build();
    }

    private void runGetInvoiceStatus(OnlineSession session, String invoiceRef,
                                     List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionInvoiceStatus response = session.invoiceStatus(invoiceRef);
            logInvoiceStatus(INVOICE_STATUS_LABEL, response);
            results.add(RunResult.ok(NAME, OP_GET_INVOICE_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICE_STATUS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void logInvoiceStatus(String label, SessionInvoiceStatus invoice) {
        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        if (invoice == null || invoice.status() == null) {
            LOGGER.info(LOG_INVOICE_STATUS_NULL, NAME, label, NO_STATUS_PLACEHOLDER);
            return;
        }
        Integer code = invoice.status().code();
        String description = invoice.status().description();
        String ksefNumber = invoice.ksefNumber() == null ? null : invoice.ksefNumber().value();
        LOGGER.info(LOG_INVOICE_STATUS, NAME, label, code, description, ksefNumber);
        List<String> details = invoice.status().details();
        if (details != null && !details.isEmpty()) {
            for (String detail : details) {
                LOGGER.info(LOG_INVOICE_STATUS_DETAIL, NAME, detail);
            }
        }
    }

    private void runGetStatus(OnlineSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            Object code = response.status() != null ? response.status().code() : NULL_LITERAL;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_SESSION_STATUS, NAME, code);
            }
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetInvoices(OnlineSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionInvoices response = session.invoices();
            int count = response.invoices() != null ? response.invoices().size() : 0;
            LOGGER.info(LOG_SESSION_INVOICES, NAME, count);
            results.add(RunResult.ok(NAME, OP_GET_INVOICES, elapsed(start),
                    count + INVOICES_LABEL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICES, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetFailedInvoices(OnlineSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionInvoices response = session.failedInvoices();
            List<SessionInvoiceStatus> failed = response.invoices() != null
                    ? response.invoices()
                    : List.of();
            LOGGER.info(LOG_FAILED_INVOICES, NAME, failed.size());
            for (SessionInvoiceStatus invoice : failed) {
                logInvoiceStatus(FAILED_INVOICE_LABEL, invoice);
            }
            results.add(RunResult.ok(NAME, OP_GET_FAILED, elapsed(start),
                    failed.size() + FAILED_LABEL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_FAILED, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession runArchive(
            DemoContext context, OnlineSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession closed = session.complete();
            LOGGER.info(LOG_SESSION_CLOSED, NAME);
            results.add(RunResult.ok(NAME, OP_CLOSE, elapsed(start)));
            return closed;
        } catch (io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException terminalFailure) {
            // KSeF terminal status 440 ("Sesja anulowana") on close is the documented
            // outcome when the session ends without any invoice being sent — see
            // ksef-docs/api-changelog.md status 'Cancelled'. AUTH_SAFE never sends, so
            // the server cancels on close. Treat as expected, not a failure.
            if (terminalFailure.code() == TERMINAL_CANCELLED_NO_INVOICES
                    && context.mode() != DemoMode.FULL) {
                results.add(RunResult.ok(NAME, OP_CLOSE, elapsed(start),
                        OK_CANCELLED_NO_INVOICES));
                return null;
            }
            results.add(RunResult.fail(NAME, OP_CLOSE, elapsed(start),
                    errorMessage(terminalFailure)));
            return null;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private void runGetCleared(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.ClosedSession closed,
                               String invoiceRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var cleared = closed.cleared(invoiceRef);
            byte[] upo = cleared.upo().xmlBytes();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_UPO_RETRIEVED, NAME, upo.length);
            }
            results.add(RunResult.ok(NAME, OP_UPO, elapsed(start),
                    upo.length + BYTES_LABEL));
            cleared.submitted().ksefNumber().ifPresent(ksefNumber ->
                    LOGGER.info(LOG_UPO_BY_KSEF_RETRIEVED, NAME, upo.length));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO, elapsed(start),
                    errorMessage(exception)));
        }
    }

}
