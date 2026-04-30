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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.model.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for batch session operations using KsefBatchSession.
 *
 * <p>Exercises the automated batch flow:
 * {@code openBatchSession(FormCode, List<byte[]>)} → {@code uploadParts()} →
 * {@code status()} → {@code close()}.</p>
 *
 * <p>FULL mode only — sends actual invoices to KSeF.</p>
 */
public final class BatchSessionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BatchSessionRunner.class);
    private static final String NAME = "batchSession";
    private static final String OP_OPEN_BATCH = "openBatchSession";
    private static final String OP_UPLOAD_PARTS = "uploadParts";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_CLOSE = "close";
    private static final String SKIP_NOT_FULL = "FULL mode only";
    private static final int INVOICE_COUNT = 3;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_OPEN_BATCH, SKIP_NOT_FULL));
            return results;
        }

        List<byte[]> invoiceXmls = generateInvoices(context.nipIdentifier());

        long openStart = System.currentTimeMillis();
        KsefBatchSession batch;
        try {
            batch = context.client().openBatchSession(FormCode.FA2, invoiceXmls);
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_BATCH, elapsed(openStart),
                    errorMessage(exception)));
            return results;
        }

        try (KsefBatchSession session = batch) {
            String batchRef = session.referenceNumber();
            LOG.info("[{}] opened batch session ref={}, invoices={}, parts={}",
                    NAME, batchRef, invoiceXmls.size(), session.partUploadRequests().size());
            results.add(RunResult.ok(NAME, OP_OPEN_BATCH, elapsed(openStart),
                    "ref=" + batchRef + ", " + invoiceXmls.size() + " invoices"));

            runUploadParts(session, results);
            runGetStatus(session, results);
            runClose(session, results);
        }

        return results;
    }

    private List<byte[]> generateInvoices(String sellerNip) {
        List<byte[]> invoices = new ArrayList<>(INVOICE_COUNT);
        for (int index = 0; index < INVOICE_COUNT; index++) {
            invoices.add(TestInvoiceXml.generate(sellerNip));
        }
        return invoices;
    }

    private void runUploadParts(KsefBatchSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.uploadParts();
            LOG.info("[{}] uploaded {} part(s)", NAME, session.partUploadRequests().size());
            results.add(RunResult.ok(NAME, OP_UPLOAD_PARTS, elapsed(start),
                    session.partUploadRequests().size() + " parts"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPLOAD_PARTS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetStatus(KsefBatchSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            LOG.info("[{}] status: code={}", NAME,
                    response.status() != null ? response.status().code() : "null");
            results.add(RunResult.ok(NAME, OP_GET_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runClose(KsefBatchSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOG.info("[{}] batch session closed (415 retry + polling handled by SDK)", NAME);
            results.add(RunResult.ok(NAME, OP_CLOSE, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
