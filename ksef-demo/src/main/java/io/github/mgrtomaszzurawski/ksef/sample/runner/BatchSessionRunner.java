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
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchAssemblyMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionStatus;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for batch session operations using KsefBatchSession.
 *
 * <p>Exercises both {@link BatchAssemblyMode} variants end-to-end:
 * <ol>
 *   <li>Default on-disk assembly ({@code java.io.tmpdir}) — confirms the
 *       streaming pipeline + {@code BodyPublishers.ofFile} upload path.</li>
 *   <li>In-memory assembly with a 50 MB heap cap — confirms the
 *       {@code BodyPublishers.ofByteArray} upload path used in
 *       restricted-filesystem deployments (read-only containers, AWS
 *       Lambda small {@code /tmp}).</li>
 * </ol>
 *
 * <p>Batch sessions do not exhibit the post-termination cooldown that
 * online sessions do (per
 * {@code context/RCA/RCA-session-cooldown-consecutive-runs-2026-04-04-2105.md}),
 * so two sessions back-to-back are safe.
 *
 * <p>FULL mode only — sends actual invoices to KSeF.
 */
public final class BatchSessionRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSessionRunner.class);
    private static final String NAME = "batchSession";
    private static final String OP_OPEN_BATCH = "openBatchSession";
    private static final String OP_UPLOAD_PARTS = "uploadParts";
    private static final String OP_GET_STATUS = "getStatus";
    private static final String OP_CLOSE = "close";
    private static final String SUFFIX_ON_DISK = "[onDisk]";
    private static final String SUFFIX_IN_MEMORY = "[inMemory]";
    private static final int INVOICE_COUNT = 3;
    private static final long IN_MEMORY_CAP_BYTES = 50L * 1024L * 1024L;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        runOnce(context, BatchSessionOptions.online(), SUFFIX_ON_DISK, results);
        runOnce(context,
                BatchSessionOptions.online().withAssembly(BatchAssemblyMode.inMemory(IN_MEMORY_CAP_BYTES)),
                SUFFIX_IN_MEMORY, results);
        return results;
    }

    private void runOnce(DemoContext context, BatchSessionOptions options,
                         String labelSuffix, List<RunResult> results) {
        List<byte[]> invoiceXmls = generateInvoices(context.nipIdentifier());

        long openStart = System.currentTimeMillis();
        KsefBatchSession batch;
        try {
            batch = context.client().openBatchSession(FormCode.FA3, invoiceXmls, options);
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_BATCH + labelSuffix, elapsed(openStart),
                    errorMessage(exception)));
            return;
        }

        try (KsefBatchSession session = batch) {
            String batchRef = session.referenceNumber();
            int invoiceCount = invoiceXmls.size();
            int partCount = session.partUploadRequests().size();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] {} opened batch session ref={}, invoices={}, parts={}",
                        NAME, labelSuffix, batchRef, invoiceCount, partCount);
            }
            results.add(RunResult.ok(NAME, OP_OPEN_BATCH + labelSuffix, elapsed(openStart),
                    "ref=" + batchRef + ", " + invoiceCount + " invoices"));

            runUploadParts(session, labelSuffix, results);
            runGetStatus(session, labelSuffix, results);
            runClose(session, labelSuffix, results);
        }
    }

    private List<byte[]> generateInvoices(String sellerNip) {
        List<byte[]> invoices = new ArrayList<>(INVOICE_COUNT);
        for (int index = 0; index < INVOICE_COUNT; index++) {
            invoices.add(TestInvoiceXml.generate(sellerNip));
        }
        return invoices;
    }

    private void runUploadParts(KsefBatchSession session, String labelSuffix, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.uploadParts();
            LOGGER.info("[{}] {} uploaded {} part(s)", NAME, labelSuffix, session.partUploadRequests().size());
            results.add(RunResult.ok(NAME, OP_UPLOAD_PARTS + labelSuffix, elapsed(start),
                    session.partUploadRequests().size() + " parts"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPLOAD_PARTS + labelSuffix, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetStatus(KsefBatchSession session, String labelSuffix, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            LOGGER.info("[{}] {} status: code={}", NAME, labelSuffix,
                    response.status() != null ? response.status().code() : "null");
            results.add(RunResult.ok(NAME, OP_GET_STATUS + labelSuffix, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS + labelSuffix, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runClose(KsefBatchSession session, String labelSuffix, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOGGER.info("[{}] {} batch session closed", NAME, labelSuffix);
            results.add(RunResult.ok(NAME, OP_CLOSE + labelSuffix, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE + labelSuffix, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
