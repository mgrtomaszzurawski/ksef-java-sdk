/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for session operations using the KsefSession API.
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
    private static final String OP_UPO_BY_KSEF = "getUpoByKsefNumber";
    private static final String OP_STALE_SESSION_RECOVERY = "staleSessionRecovery";

    private static final String SKIP_NOT_FULL = "FULL mode only";
    private static final String SKIP_NO_KSEF_NUMBER =
            "invoice has no ksefNumber (likely rejected) — cannot fetch UPO by KSeF number";
    private static final String FAIL_UPO_BYTES_DIFFER =
            "UPO retrieved by invoice ref differs from UPO retrieved by KSeF number";
    private static final String OK_CONCURRENT_PERMITTED = "server permitted concurrent open";
    private static final String OK_CONCURRENT_REJECTED_PREFIX = "server rejected: ";

    private static final String REF_PREFIX = "ref=";
    private static final String INVOICES_LABEL = " invoices";
    private static final String FAILED_LABEL = " failed";
    private static final String BYTES_LABEL = " bytes";
    private static final String BYTES_MATCHES_LABEL = " bytes, matches by-ref UPO";
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

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_SESSION, SKIP_NOT_FULL));
            return results;
        }

        runStaleSessionRecovery(context, results);

        KsefSession session = runOpenSession(context, results);
        if (session == null) {
            return results;
        }

        try (session) {
            String invoiceRef = runSendInvoice(context, session, results);
            if (invoiceRef != null) {
                runGetInvoiceStatus(session, invoiceRef, results);
            }
            runGetStatus(session, results);
            runGetInvoices(session, results);
            runGetFailedInvoices(session, results);
            boolean closed = runClose(session, results);
            if (closed && invoiceRef != null) {
                byte[] upoByInvoiceRef = runGetUpo(session, invoiceRef, results);
                runGetUpoByKsefNumber(context, session, invoiceRef, upoByInvoiceRef, results);
            }
        }
        return results;
    }

    /**
     * Records whether the server permits a second concurrent open for the same
     * NIP. Both outcomes are valid observations. Both probe sessions are
     * closed in {@code finally} before the happy-path test runs.
     */
    private void runStaleSessionRecovery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefSession firstSession = null;
        KsefSession secondSession = null;
        try {
            firstSession = context.client().openSession(FormCode.FA2);
            LOGGER.info(LOG_FIRST_SESSION_OPENED, NAME, firstSession.referenceNumber());
            try {
                secondSession = context.client().openSession(FormCode.FA2);
                LOGGER.info(LOG_CONCURRENT_PERMITTED, NAME, secondSession.referenceNumber());
                results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                        OK_CONCURRENT_PERMITTED));
            } catch (Exception rejected) {
                LOGGER.info(LOG_CONCURRENT_REJECTED, NAME, rejected.getClass().getSimpleName());
                results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                        OK_CONCURRENT_REJECTED_PREFIX + rejected.getClass().getSimpleName()));
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                    errorMessage(exception)));
        } finally {
            quietClose(secondSession);
            quietClose(firstSession);
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
    private KsefSession runOpenSession(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            KsefSession session = context.client().openSession(FormCode.FA2);
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

    private String runSendInvoice(DemoContext context, KsefSession session,
                                  List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            byte[] invoiceXml = TestInvoiceXml.generate(context.nipIdentifier());
            SendInvoiceResult sendResult = session.send(invoiceXml);
            String invoiceRef = sendResult.referenceNumber();
            LOGGER.info(LOG_SENT_INVOICE, NAME, invoiceRef);
            results.add(RunResult.ok(NAME, OP_SEND_INVOICE, elapsed(start),
                    REF_PREFIX + invoiceRef));
            return invoiceRef;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SEND_INVOICE, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private void runGetInvoiceStatus(KsefSession session, String invoiceRef,
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
        if (invoice == null || invoice.status() == null) {
            LOGGER.info(LOG_INVOICE_STATUS_NULL, NAME, label, NO_STATUS_PLACEHOLDER);
            return;
        }
        LOGGER.info(LOG_INVOICE_STATUS,
                NAME, label,
                invoice.status().code(),
                invoice.status().description(),
                invoice.ksefNumber());
        List<String> details = invoice.status().details();
        if (details != null && !details.isEmpty()) {
            for (String detail : details) {
                LOGGER.info(LOG_INVOICE_STATUS_DETAIL, NAME, detail);
            }
        }
    }

    private void runGetStatus(KsefSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            Object code = response.status() != null ? response.status().code() : NULL_LITERAL;
            LOGGER.info(LOG_SESSION_STATUS, NAME, code);
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetInvoices(KsefSession session, List<RunResult> results) {
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

    private void runGetFailedInvoices(KsefSession session, List<RunResult> results) {
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

    private boolean runClose(KsefSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOGGER.info(LOG_SESSION_CLOSED, NAME);
            results.add(RunResult.ok(NAME, OP_CLOSE, elapsed(start)));
            return true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE, elapsed(start),
                    errorMessage(exception)));
            return false;
        }
    }

    private byte[] runGetUpo(KsefSession session, String invoiceRef,
                             List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            byte[] upo = session.upo(invoiceRef);
            LOGGER.info(LOG_UPO_RETRIEVED, NAME, upo.length);
            results.add(RunResult.ok(NAME, OP_UPO, elapsed(start),
                    upo.length + BYTES_LABEL));
            return upo;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    /**
     * Re-queries the invoice status to get the assigned KSeF number, then
     * fetches UPO by KSeF number and confirms it matches the UPO retrieved by
     * invoice reference. After close completes, the ksefNumber is populated for
     * accepted invoices; rejected invoices leave it null (skipped).
     */
    private void runGetUpoByKsefNumber(DemoContext context, KsefSession session,
                                       String invoiceRef, byte[] upoByInvoiceRef,
                                       List<RunResult> results) {
        long start = System.currentTimeMillis();
        String ksefNumber;
        try {
            SessionInvoiceStatus status = session.invoiceStatus(invoiceRef);
            ksefNumber = status.ksefNumber();
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO_BY_KSEF, elapsed(start),
                    errorMessage(exception)));
            return;
        }

        if (ksefNumber == null || ksefNumber.isBlank()) {
            results.add(RunResult.skip(NAME, OP_UPO_BY_KSEF, SKIP_NO_KSEF_NUMBER));
            return;
        }
        context.setInvoiceKsefNumber(ksefNumber);

        try {
            byte[] upoByKsef = new SessionClient(context.client()).getUpoByKsefNumber(
                    session.referenceNumber(), ksefNumber);
            LOGGER.info(LOG_UPO_BY_KSEF_RETRIEVED, NAME, upoByKsef.length);

            if (upoByInvoiceRef != null && !Arrays.equals(upoByInvoiceRef, upoByKsef)) {
                results.add(RunResult.fail(NAME, OP_UPO_BY_KSEF, elapsed(start),
                        FAIL_UPO_BYTES_DIFFER));
                return;
            }
            results.add(RunResult.ok(NAME, OP_UPO_BY_KSEF, elapsed(start),
                    upoByKsef.length + BYTES_MATCHES_LABEL));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO_BY_KSEF, elapsed(start),
                    errorMessage(exception)));
        }
    }

    /**
     * Best-effort cleanup — failures here would mask the underlying test result
     * so they are intentionally swallowed.
     */
    private static void quietClose(KsefSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (Exception ignored) {
            // intentionally ignored
        }
    }
}
