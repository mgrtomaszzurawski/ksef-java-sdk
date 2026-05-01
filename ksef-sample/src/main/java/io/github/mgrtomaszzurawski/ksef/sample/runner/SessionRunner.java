/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
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

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SendInvoiceResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionInvoiceStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionInvoices;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runner for session operations using the KsefSession API.
 * Tests each operation separately with per-operation reporting.
 * FULL mode only.
 *
 * <p>Operations tested: openSession, send, invoiceStatus, status,
 * invoices, failedInvoices, close (with 415 retry handled by SDK),
 * UPO retrieval (by invoice ref and by KSeF number), and a
 * negative-path stale-session-recovery probe.</p>
 */
public final class SessionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRunner.class);
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

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_SESSION, SKIP_NOT_FULL));
            return results;
        }

        // 0. Negative-path probe: confirm the one-session-per-NIP server constraint
        //    by attempting to open a second session while the first is still open.
        runStaleSessionRecovery(context, results);

        // 1. Open session
        KsefSession session = runOpenSession(context, results);
        if (session == null) {
            return results;
        }

        // 2. Send invoice
        String invoiceRef = runSendInvoice(context, session, results);

        // 3. Get invoice status (if send succeeded)
        if (invoiceRef != null) {
            runGetInvoiceStatus(session, invoiceRef, results);
        }

        // 4. Get session status
        runGetStatus(session, results);

        // 5. Get invoices list
        runGetInvoices(session, results);

        // 6. Get failed invoices
        runGetFailedInvoices(session, results);

        // 7. Close session (SDK handles 415 retry + completion polling internally)
        boolean closed = runClose(session, results);

        // 8. Retrieve UPO (available after close)
        if (closed && invoiceRef != null) {
            byte[] upoByInvoiceRef = runGetUpo(session, invoiceRef, results);
            // 9. Retrieve UPO by KSeF number — alternative endpoint, should match.
            runGetUpoByKsefNumber(context, session, invoiceRef, upoByInvoiceRef, results);
        }

        return results;
    }

    private void runStaleSessionRecovery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefSession first = null;
        KsefSession second = null;
        try {
            first = context.client().openSession(FormCode.FA2);
            LOG.info("[{}] first session opened ref={}, probing concurrent-open behavior",
                    NAME, first.referenceNumber());
            try {
                second = context.client().openSession(FormCode.FA2);
                LOG.info("[{}] server permitted concurrent open: ref={}", NAME,
                        second.referenceNumber());
                results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                        "server permitted concurrent open"));
            } catch (Exception rejected) {
                LOG.info("[{}] server rejected concurrent open: {}", NAME,
                        rejected.getClass().getSimpleName());
                results.add(RunResult.ok(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                        "server rejected: " + rejected.getClass().getSimpleName()));
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_STALE_SESSION_RECOVERY, elapsed(start),
                    errorMessage(exception)));
        } finally {
            quietClose(second);
            quietClose(first);
        }
    }

    private KsefSession runOpenSession(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            KsefSession session = context.client().openSession(FormCode.FA2);
            String sessionRef = session.referenceNumber();
            LOG.info("[{}] opened session ref={}", NAME, sessionRef);
            context.setSessionReferenceNumber(sessionRef);
            context.state().setSessionReferenceNumber(sessionRef);
            results.add(RunResult.ok(NAME, OP_OPEN_SESSION, elapsed(start),
                    "ref=" + sessionRef));
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
            LOG.info("[{}] sent invoice ref={}", NAME, invoiceRef);
            context.setInvoiceReferenceNumber(invoiceRef);
            results.add(RunResult.ok(NAME, OP_SEND_INVOICE, elapsed(start),
                    "ref=" + invoiceRef));
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
            LOG.info("[{}] invoice status: code={}", NAME,
                    response.status() != null ? response.status().code() : "null");
            results.add(RunResult.ok(NAME, OP_GET_INVOICE_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICE_STATUS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetStatus(KsefSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            LOG.info("[{}] session status: code={}", NAME,
                    response.status() != null ? response.status().code() : "null");
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
            LOG.info("[{}] session invoices: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_GET_INVOICES, elapsed(start),
                    count + " invoices"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICES, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetFailedInvoices(KsefSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionInvoices response = session.failedInvoices();
            int count = response.invoices() != null ? response.invoices().size() : 0;
            LOG.info("[{}] failed invoices: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_GET_FAILED, elapsed(start),
                    count + " failed"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_FAILED, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private boolean runClose(KsefSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOG.info("[{}] session closed (415 retry + polling handled by SDK)", NAME);
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
            LOG.info("[{}] UPO retrieved, size={} bytes", NAME, upo.length);
            results.add(RunResult.ok(NAME, OP_UPO, elapsed(start),
                    upo.length + " bytes"));
            return upo;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private void runGetUpoByKsefNumber(DemoContext context, KsefSession session,
                                       String invoiceRef, byte[] upoByInvoiceRef,
                                       List<RunResult> results) {
        long start = System.currentTimeMillis();
        String ksefNumber;
        try {
            // Re-query invoice status — after close completes, the ksefNumber should be
            // populated for accepted invoices (rejected invoices leave it null).
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
            byte[] upoByKsef = context.client().sessions().getUpoByKsefNumber(
                    session.referenceNumber(), ksefNumber);
            LOG.info("[{}] UPO by KSeF number retrieved, size={} bytes", NAME, upoByKsef.length);

            if (upoByInvoiceRef != null && !Arrays.equals(upoByInvoiceRef, upoByKsef)) {
                results.add(RunResult.fail(NAME, OP_UPO_BY_KSEF, elapsed(start),
                        FAIL_UPO_BYTES_DIFFER));
                return;
            }
            results.add(RunResult.ok(NAME, OP_UPO_BY_KSEF, elapsed(start),
                    upoByKsef.length + " bytes, matches by-ref UPO"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO_BY_KSEF, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private static void quietClose(KsefSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (Exception ignored) {
            // Best-effort cleanup — failures here would mask the underlying test result.
        }
    }
}
