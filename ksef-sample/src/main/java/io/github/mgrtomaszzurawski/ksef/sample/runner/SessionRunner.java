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

import io.github.mgrtomaszzurawski.ksef.client.model.EncryptionInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OpenOnlineSessionResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SendInvoiceResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner for SessionClient operations. Opens an online session, sends ONE test invoice,
 * queries status, closes, and polls for UPO. FULL mode only.
 */
public final class SessionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SessionRunner.class);
    private static final String NAME = "session";
    private static final String OP_OPEN_ONLINE = "openOnline";
    private static final String OP_SEND_INVOICE = "sendInvoice";
    private static final String OP_GET_INVOICE_STATUS = "getInvoiceStatus";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_GET_INVOICES = "getInvoices";
    private static final String OP_GET_FAILED = "getFailedInvoices";
    private static final String OP_CLOSE_ONLINE = "closeOnline";
    private static final String OP_OPEN_BATCH = "openBatch";
    private static final String OP_CLOSE_BATCH = "closeBatch";
    private static final String SKIP_BATCH = "requires ZIP package preparation";
    private static final String SKIP_NOT_FULL = "FULL mode only";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String OP_POLL_SESSION = "pollSessionStatus";
    private static final int SESSION_STATUS_OK = 200;
    private static final int POLL_INITIAL_DELAY_MS = 1000;
    private static final int POLL_MAX_DELAY_MS = 5000;
    private static final int POLL_TIMEOUT_MS = 60000;
    private static final int POLL_BACKOFF_MULTIPLIER = 2;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_ONLINE, SKIP_NOT_FULL));
            return results;
        }

        // 1. Open online session
        String sessionRef = runOpenOnline(context, results);
        if (sessionRef == null) {
            return results;
        }

        // 2. Send one test invoice
        String invoiceRef = runSendInvoice(context, sessionRef, results);

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
        runCloseOnlineWithRetry(context, sessionRef, results);

        // Batch — skipped
        results.add(RunResult.skip(NAME, OP_OPEN_BATCH, SKIP_BATCH));
        results.add(RunResult.skip(NAME, OP_CLOSE_BATCH, SKIP_BATCH));

        return results;
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

            // Store AES key+IV for invoice encryption
            context.state().setSessionReferenceNumber(refNum);

            results.add(RunResult.ok(NAME, OP_OPEN_ONLINE, elapsed(start), "ref=" + refNum));

            // Store encryption params in context for sendInvoice
            context.setAesKey(aesKey);
            context.setInitVector(initVector);
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_ONLINE, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runSendInvoice(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
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
            LOG.info("[{}] sent invoice, ref={}", NAME, refNum);
            context.setInvoiceReferenceNumber(refNum);
            results.add(RunResult.ok(NAME, OP_SEND_INVOICE, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SEND_INVOICE, elapsed(start), errorMessage(exception)));
            return null;
        }
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

    private void pollSessionStatus(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        try {
            while (elapsed(start) < POLL_TIMEOUT_MS) {
                var response = context.client().sessions().getStatus(sessionRef);
                Integer code = response.getStatus() != null ? response.getStatus().getCode() : null;
                LOG.info("[{}] session status: code={}", NAME, code);
                if (code != null && code == SESSION_STATUS_OK) {
                    results.add(RunResult.ok(NAME, OP_POLL_SESSION, elapsed(start),
                            "ready after " + elapsed(start) + "ms"));
                    return;
                }
                Thread.sleep(delay);
                delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
            }
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start),
                    "Timeout waiting for session status 200"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start), "Interrupted"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_POLL_SESSION, elapsed(start), errorMessage(exception)));
        }
    }

    private void runCloseOnlineWithRetry(DemoContext context, String sessionRef, List<RunResult> results) {
        long start = System.currentTimeMillis();
        int delay = POLL_INITIAL_DELAY_MS;
        while (elapsed(start) < POLL_TIMEOUT_MS) {
            try {
                context.client().sessions().closeOnline(sessionRef);
                LOG.info("[{}] closed online session ref={}", NAME, sessionRef);
                results.add(RunResult.ok(NAME, OP_CLOSE_ONLINE, elapsed(start)));
                return;
            } catch (Exception exception) {
                String msg = exception.getMessage();
                if (msg != null && msg.contains("415")) {
                    LOG.info("[{}] session still processing, retrying close in {}ms", NAME, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start), "Interrupted"));
                        return;
                    }
                    delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
                } else {
                    results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start), errorMessage(exception)));
                    return;
                }
            }
        }
        results.add(RunResult.fail(NAME, OP_CLOSE_ONLINE, elapsed(start),
                "Timeout waiting for session to be closeable"));
    }
}
