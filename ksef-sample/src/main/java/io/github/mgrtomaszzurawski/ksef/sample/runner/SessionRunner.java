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

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_BACKOFF_MULTIPLIER;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_INITIAL_DELAY_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_MAX_DELAY_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.POLL_TIMEOUT_MS;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SessionStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner for SessionClient operations. Opens an online session, sends ONE test invoice,
 * queries status, closes, polls for completion, and retrieves UPO. FULL mode only.
 *
 * <p>Before opening a new session, attempts to terminate any stale session left from a
 * previous run (KSeF allows only one active online session per NIP).</p>
 */
public final class SessionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRunner.class);
    private static final String NAME = "session";
    private static final String OP_CLEANUP_STALE = "cleanupStaleSession";
    private static final String OP_OPEN_ONLINE = "openOnline";
    private static final String OP_SEND_INVOICE = "sendInvoice";
    private static final String OP_GET_INVOICE_STATUS = "getInvoiceStatus";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_GET_INVOICES = "getInvoices";
    private static final String OP_GET_FAILED = "getFailedInvoices";
    private static final String OP_CLOSE_ONLINE = "closeOnline";
    private static final String OP_POLL_SESSION = "pollSessionStatus";
    private static final String OP_UPO_BY_REF = "getUpoByInvoiceReference";
    private static final String OP_TERMINATE_FALLBACK = "terminateFallback";
    private static final String OP_OPEN_BATCH = "openBatch";
    private static final String OP_CLOSE_BATCH = "closeBatch";
    private static final String SKIP_BATCH = "requires ZIP package preparation";
    private static final String SKIP_NOT_FULL = "FULL mode only";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int SESSION_STATUS_OK = 200;
    private static final int SEND_RETRY_DELAY_MS = 2000;
    private static final int SEND_MAX_RETRIES = 3;
    private static final String SESSION_BUSY_INDICATOR = "(415)";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_ONLINE, SKIP_NOT_FULL));
            return results;
        }

        // 1. Open online session (with stale session cleanup if needed)
        String sessionRef = runOpenOnlineWithCleanup(context, results);
        if (sessionRef == null) {
            return results;
        }

        // 2. Send one test invoice (with retry)
        String invoiceRef = runSendInvoiceWithRetry(context, sessionRef, results);

        // 3. Get invoice status
        if (invoiceRef != null) {
            runGetInvoiceStatus(context, sessionRef, invoiceRef, results);
        }

        // 4. Get session status
        runGetStatus(context, sessionRef, results);

        // 5. Get invoices list
        runGetInvoices(context, sessionRef, results);

        // 6. Get failed invoices
        runGetFailedInvoices(context, sessionRef, results);

        // 7. Close session (with retry — waits for invoice processing)
        boolean closed = runCloseOnlineWithRetry(context, sessionRef, results);

        if (!closed) {
            // Fallback: terminate the auth session to clean up the stuck online session
            terminateFallback(context, results);
        }

        // 8. Poll session status after close until processing complete
        if (closed) {
            boolean ready = pollSessionStatusAfterClose(context, sessionRef, results);

            // 9. Retrieve UPO by invoice reference
            if (ready && invoiceRef != null) {
                runGetUpoByInvoiceReference(context, sessionRef, invoiceRef, results);
            }
        }

        // Batch — skipped
        results.add(RunResult.skip(NAME, OP_OPEN_BATCH, SKIP_BATCH));
        results.add(RunResult.skip(NAME, OP_CLOSE_BATCH, SKIP_BATCH));

        return results;
    }

    /**
     * Try to open online session. If it fails (likely because a stale session exists
     * from a previous run), terminate the auth session, re-authenticate, and retry.
     *
     * <p>Note: KSeF has a cooldown period after session termination. Consecutive FULL
     * runs within the same minute may fail because KSeF hasn't released the session slot.
     * This is documented as a known server behavior.</p>
     */
    private String runOpenOnlineWithCleanup(DemoContext context, List<RunResult> results) {
        String sessionRef = runOpenOnline(context, results);
        if (sessionRef != null) {
            return sessionRef;
        }

        // Open failed — likely stale session blocking. Terminate and retry.
        LOG.info("[{}] openOnline failed, attempting cleanup and retry", NAME);
        long start = System.currentTimeMillis();
        try {
            context.client().auth().terminateCurrentSession();
            LOG.info("[{}] terminated stale session", NAME);
            reAuthenticate(context);
            results.add(RunResult.ok(NAME, OP_CLEANUP_STALE, elapsed(start), "stale session terminated"));
        } catch (Exception exception) {
            LOG.warn("[{}] cleanup failed: {}", NAME, exception.getMessage());
            results.add(RunResult.fail(NAME, OP_CLEANUP_STALE, elapsed(start), errorMessage(exception)));
            return null;
        }

        // Retry open after cleanup
        return runOpenOnline(context, results);
    }

    /**
     * Re-authenticate after terminating a stale session. The terminate call invalidates
     * the auth session, so we need a fresh one.
     */
    private void reAuthenticate(DemoContext context) throws Exception {
        var challenge = context.client().auth().requestChallenge();
        context.client().auth().authenticateWithToken(challenge, context.ksefToken(),
                context.nipIdentifier(), context.ksefPublicKey());

        // Poll auth status
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        while (elapsed(start) < POLL_TIMEOUT_MS) {
            var status = context.client().auth().getStatus(
                    context.client().sessionContext().referenceNumber());
            Integer code = status.getStatus() != null ? status.getStatus().getCode() : null;
            if (code != null && code == SESSION_STATUS_OK) {
                break;
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }

        context.client().auth().redeemTokens();
        LOG.info("[{}] re-authenticated after stale session cleanup", NAME);
    }

    private String runOpenOnline(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            byte[] aesKey = CryptoService.generateAesKey();
            byte[] initVector = CryptoService.generateIv();
            byte[] encryptedKey = CryptoService.encryptWithPublicKey(aesKey, context.ksefPublicKey());

            OpenOnlineSessionRequestRaw request = new OpenOnlineSessionRequestRaw()
                    .formCode(new FormCodeRaw()
                            .systemCode(TestInvoiceXml.systemCode())
                            .schemaVersion(TestInvoiceXml.schemaVersion())
                            .value(TestInvoiceXml.formCodeValue()))
                    .encryption(new EncryptionInfoRaw()
                            .encryptedSymmetricKey(encryptedKey)
                            .initializationVector(initVector));

            OpenOnlineSessionResponseRaw response = context.client().sessions().openOnline(request);
            String refNum = response.getReferenceNumber();
            LOG.info("[{}] opened online session, ref={}", NAME, refNum);
            context.setSessionReferenceNumber(refNum);
            context.state().setSessionReferenceNumber(refNum);
            context.setAesKey(aesKey);
            context.setInitVector(initVector);

            results.add(RunResult.ok(NAME, OP_OPEN_ONLINE, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_ONLINE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    /**
     * Send invoice with retry. The session might need a moment after openOnline.
     */
    private String runSendInvoiceWithRetry(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        Exception lastException = null;

        for (int attempt = 1; attempt <= SEND_MAX_RETRIES; attempt++) {
            try {
                byte[] invoiceXml = TestInvoiceXml.generate(context.nipIdentifier());
                byte[] invoiceHash = MessageDigest.getInstance(SHA256_ALGORITHM).digest(invoiceXml);

                byte[] encryptedInvoice = CryptoService.encryptAes(invoiceXml,
                        context.aesKey(), context.initVector());
                byte[] encryptedHash = MessageDigest.getInstance(SHA256_ALGORITHM).digest(encryptedInvoice);

                SendInvoiceRequestRaw request = new SendInvoiceRequestRaw()
                        .invoiceHash(invoiceHash)
                        .invoiceSize((long) invoiceXml.length)
                        .encryptedInvoiceHash(encryptedHash)
                        .encryptedInvoiceSize((long) encryptedInvoice.length)
                        .encryptedInvoiceContent(encryptedInvoice);

                SendInvoiceResponseRaw response = context.client().sessions()
                        .sendInvoice(sessionRef, request);
                String refNum = response.getReferenceNumber();
                LOG.info("[{}] sent invoice, ref={} (attempt {})", NAME, refNum, attempt);
                context.setInvoiceReferenceNumber(refNum);
                results.add(RunResult.ok(NAME, OP_SEND_INVOICE, elapsed(start), "ref=" + refNum));
                return refNum;
            } catch (Exception exception) {
                lastException = exception;
                LOG.warn("[{}] sendInvoice attempt {} failed: {}", NAME, attempt, exception.getMessage());
                if (attempt < SEND_MAX_RETRIES) {
                    try {
                        Thread.sleep(SEND_RETRY_DELAY_MS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        results.add(RunResult.fail(NAME, OP_SEND_INVOICE, elapsed(start), "Interrupted"));
                        return null;
                    }
                }
            }
        }

        results.add(RunResult.fail(NAME, OP_SEND_INVOICE, elapsed(start), errorMessage(lastException)));
        return null;
    }

    private void runGetInvoiceStatus(DemoContext context, String sessionRef,
                                     String invoiceRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().sessions().getInvoiceStatus(sessionRef, invoiceRef);
            LOG.info("[{}] invoice status: code={}", NAME,
                    response.getStatus() != null ? response.getStatus().getCode() : "null");
            results.add(RunResult.ok(NAME, OP_GET_INVOICE_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICE_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGetStatus(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().sessions().getStatus(sessionRef);
            LOG.info("[{}] session status: code={}", NAME,
                    response.getStatus() != null ? response.getStatus().getCode() : "null");
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGetInvoices(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().sessions().getInvoices(sessionRef);
            int count = response.getInvoices() != null ? response.getInvoices().size() : 0;
            LOG.info("[{}] session invoices: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_GET_INVOICES, elapsed(start), count + " invoices"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_INVOICES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGetFailedInvoices(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().sessions().getFailedInvoices(sessionRef);
            int count = response.getInvoices() != null ? response.getInvoices().size() : 0;
            LOG.info("[{}] failed invoices: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_GET_FAILED, elapsed(start), count + " failed"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_FAILED, elapsed(start), errorMessage(exception)));
        }
    }

    /**
     * Close online session with retry. Checks responseBody() for "415" (session busy/processing)
     * rather than getMessage() which only has "Unexpected HTTP status: 400".
     */
    private boolean runCloseOnlineWithRetry(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        while (elapsed(start) < POLL_TIMEOUT_MS) {
            try {
                context.client().sessions().closeOnline(sessionRef);
                LOG.info("[{}] closed online session ref={}", NAME, sessionRef);
                results.add(RunResult.ok(NAME, OP_CLOSE_ONLINE, elapsed(start)));
                return true;
            } catch (Exception exception) {
                boolean isSessionBusy = isSessionBusyError(exception);
                if (isSessionBusy) {
                    LOG.info("[{}] session still processing (415), retrying close in {}ms", NAME, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start), "Interrupted"));
                        return false;
                    }
                    delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
                } else {
                    results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start), errorMessage(exception)));
                    return false;
                }
            }
        }
        results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start),
                "Timeout waiting for session to be closeable"));
        return false;
    }

    /**
     * Fallback: terminate the auth session when closeOnline fails. This implicitly
     * closes the online session, preventing stale sessions from blocking future runs.
     * Re-authenticates afterwards so subsequent runners still have a valid token.
     */
    private void terminateFallback(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().auth().terminateCurrentSession();
            LOG.info("[{}] terminated auth session as close fallback", NAME);

            // Re-authenticate so subsequent runners (InvoiceRunner) have a valid token
            reAuthenticate(context);
            results.add(RunResult.ok(NAME, OP_TERMINATE_FALLBACK, elapsed(start),
                    "auth session terminated + re-authenticated"));
        } catch (Exception exception) {
            LOG.warn("[{}] terminate fallback failed: {}", NAME, exception.getMessage());
            results.add(RunResult.fail(NAME, OP_TERMINATE_FALLBACK, elapsed(start),
                    errorMessage(exception)));
        }
    }

    /**
     * Check if the exception indicates the session is still busy (status 415 in response body).
     * KsefException.getMessage() says "Unexpected HTTP status: 400" but the responseBody()
     * contains the actual error with status 415.
     */
    private boolean isSessionBusyError(Exception exception) {
        if (exception instanceof KsefException ksefEx) {
            String body = ksefEx.responseBody();
            if (body != null && body.contains(SESSION_BUSY_INDICATOR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Poll session status after close until processing is complete (status 200).
     * UPO is only available after the session finishes processing.
     */
    private boolean pollSessionStatusAfterClose(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        try {
            while (elapsed(start) < POLL_TIMEOUT_MS) {
                SessionStatusResponseRaw response = context.client().sessions().getStatus(sessionRef);
                Integer code = response.getStatus() != null ? response.getStatus().getCode() : null;
                LOG.info("[{}] post-close session status: code={}", NAME, code);
                if (code != null && code == SESSION_STATUS_OK) {
                    results.add(RunResult.ok(NAME, OP_POLL_SESSION, elapsed(start),
                            "ready after " + elapsed(start) + "ms"));
                    return true;
                }
                Thread.sleep(delay);
                delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
            }
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start),
                    "Timeout waiting for session status 200"));
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start), "Interrupted"));
            return false;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start), errorMessage(exception)));
            return false;
        }
    }

    /**
     * Retrieve UPO by invoice reference number. UPO is the official receipt confirming
     * invoice delivery to KSeF.
     */
    private void runGetUpoByInvoiceReference(DemoContext context, String sessionRef,
                                              String invoiceRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            byte[] upoBytes = context.client().sessions()
                    .getUpoByInvoiceReference(sessionRef, invoiceRef);
            LOG.info("[{}] UPO retrieved, size={} bytes", NAME, upoBytes.length);
            results.add(RunResult.ok(NAME, OP_UPO_BY_REF, elapsed(start),
                    upoBytes.length + " bytes"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPO_BY_REF, elapsed(start), errorMessage(exception)));
        }
    }
}
