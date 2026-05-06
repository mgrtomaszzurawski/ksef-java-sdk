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
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
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
 * Runner for batch session operations using {@code KsefBatchSession}.
 *
 * <p>Exercises the full cross-product of supported {@link FormCode}
 * variants and {@link BatchAssemblyMode} variants:
 * <ul>
 *   <li>FormCodes: FA(2), FA(3), PEF(3), PEF_KOR(3).</li>
 *   <li>Assembly modes: on-disk and in-memory.</li>
 * </ul>
 *
 * <p>FormCodes rejected by the active environment surface as SKIPs in
 * the report rather than FAILs (e.g. FA(2) on DEMO/PROD per
 * {@code srodowiska.md}).
 *
 * <p>Batch sessions do not exhibit the post-termination cooldown that
 * online sessions do, so the cross-product runs back-to-back safely.
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
    private static final String SKIP_REASON_PREFIX = "FormCode rejected by environment: ";
    private static final String SKIP_REASON_PEF_AUTH =
            "FA_PEF/FA_KOR_PEF require Peppol provider auth (PefInvoiceWrite permission) — "
                    + "demo creds use regular NIP, KSeF returns 21405 \"kod formularza nie jest wspierany\". "
                    + "Wire-shape coverage for these FormCodes lives in WireMock contract tests.";
    private static final int INVOICE_COUNT = 3;
    private static final long IN_MEMORY_CAP_BYTES = 50L * 1024L * 1024L;

    private static final String HOST_FRAGMENT_TEST = "api-test.";
    private static final String HOST_FRAGMENT_DEMO = "api-demo.";
    private static final String HOST_FRAGMENT_PREPROD = "api-preprod.";
    private static final String HOST_PREFIX_PROD = "https://api.ksef.mf.gov.pl";

    private static final List<FormCode> FORM_CODES = List.of(
            FormCode.FA2, FormCode.FA3, FormCode.PEF3, FormCode.PEF_KOR3);

    private static final List<AssemblyVariant> ASSEMBLY_VARIANTS = List.of(
            new AssemblyVariant(SUFFIX_ON_DISK, BatchSessionOptions.online()),
            new AssemblyVariant(SUFFIX_IN_MEMORY,
                    BatchSessionOptions.online().withAssembly(BatchAssemblyMode.inMemory(IN_MEMORY_CAP_BYTES))));

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        KsefEnvironment env = resolveEnv(context.environment());
        for (FormCode formCode : FORM_CODES) {
            String formCodeLabel = formCodeLabel(formCode);
            try {
                formCode.assertAllowedOn(env);
            } catch (IllegalArgumentException notAllowed) {
                for (AssemblyVariant variant : ASSEMBLY_VARIANTS) {
                    results.add(RunResult.skip(NAME, OP_OPEN_BATCH + formCodeLabel + variant.suffix(),
                            SKIP_REASON_PREFIX + notAllowed.getMessage()));
                }
                continue;
            }
            if (requiresPeppolProviderAuth(formCode)) {
                for (AssemblyVariant variant : ASSEMBLY_VARIANTS) {
                    results.add(RunResult.skip(NAME, OP_OPEN_BATCH + formCodeLabel + variant.suffix(),
                            SKIP_REASON_PEF_AUTH));
                }
                continue;
            }
            for (AssemblyVariant variant : ASSEMBLY_VARIANTS) {
                runOnce(context, formCode, formCodeLabel, variant, results);
            }
        }
        return results;
    }

    private void runOnce(DemoContext context, FormCode formCode, String formCodeLabel,
                         AssemblyVariant variant, List<RunResult> results) {
        String label = formCodeLabel + variant.suffix();
        List<byte[]> invoiceXmls = generateInvoices(formCode, context.nipIdentifier());

        long openStart = System.currentTimeMillis();
        KsefBatchSession batch;
        try {
            batch = context.client().openBatchSession(formCode, invoiceXmls, variant.options());
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_OPEN_BATCH + label, elapsed(openStart),
                    errorMessage(exception)));
            return;
        }

        try (KsefBatchSession session = batch) {
            String batchRef = session.referenceNumber();
            int invoiceCount = invoiceXmls.size();
            int partCount = session.partUploadRequests().size();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] {} opened batch session ref={}, invoices={}, parts={}",
                        NAME, label, batchRef, invoiceCount, partCount);
            }
            results.add(RunResult.ok(NAME, OP_OPEN_BATCH + label, elapsed(openStart),
                    "ref=" + batchRef + ", " + invoiceCount + " invoices"));

            runUploadParts(session, label, results);
            runGetStatus(session, label, results);
            runClose(session, label, results);
        }
    }

    private List<byte[]> generateInvoices(FormCode formCode, String sellerNip) {
        List<byte[]> invoices = new ArrayList<>(INVOICE_COUNT);
        for (int index = 0; index < INVOICE_COUNT; index++) {
            invoices.add(TestInvoiceXml.generate(formCode, sellerNip));
        }
        return invoices;
    }

    private static String formCodeLabel(FormCode formCode) {
        return "[" + formCode.systemCode().replace(" ", "") + "]";
    }

    private static boolean requiresPeppolProviderAuth(FormCode formCode) {
        return formCode.equals(FormCode.PEF3) || formCode.equals(FormCode.PEF_KOR3);
    }

    private static KsefEnvironment resolveEnv(String envUrl) {
        if (envUrl == null) {
            return KsefEnvironment.PROD;
        }
        if (envUrl.contains(HOST_FRAGMENT_TEST)) {
            return KsefEnvironment.TEST;
        }
        if (envUrl.contains(HOST_FRAGMENT_DEMO)) {
            return KsefEnvironment.DEMO;
        }
        if (envUrl.contains(HOST_FRAGMENT_PREPROD)) {
            return KsefEnvironment.PREPROD;
        }
        if (envUrl.startsWith(HOST_PREFIX_PROD)) {
            return KsefEnvironment.PROD;
        }
        return KsefEnvironment.PROD;
    }

    private void runUploadParts(KsefBatchSession session, String label, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.uploadParts();
            LOGGER.info("[{}] {} uploaded {} part(s)", NAME, label, session.partUploadRequests().size());
            results.add(RunResult.ok(NAME, OP_UPLOAD_PARTS + label, elapsed(start),
                    session.partUploadRequests().size() + " parts"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPLOAD_PARTS + label, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runGetStatus(KsefBatchSession session, String label, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SessionStatus response = session.status();
            LOGGER.info("[{}] {} status: code={}", NAME, label,
                    response.status() != null ? response.status().code() : "null");
            results.add(RunResult.ok(NAME, OP_GET_STATUS + label, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_STATUS + label, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runClose(KsefBatchSession session, String label, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOGGER.info("[{}] {} batch session closed", NAME, label);
            results.add(RunResult.ok(NAME, OP_CLOSE + label, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE + label, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private record AssemblyVariant(String suffix, BatchSessionOptions options) { }
}
