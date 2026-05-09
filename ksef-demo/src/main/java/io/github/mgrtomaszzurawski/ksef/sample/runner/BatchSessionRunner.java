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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for batch session operations using the new
 * {@code Invoices.submitBatch(...)} synchronous facade (PR11).
 *
 * <p>Exercises the supported {@link FormCode} variants:
 * <ul>
 *   <li>FormCodes: FA(2), FA(3), PEF(3), PEF_KOR(3).</li>
 * </ul>
 *
 * <p>FormCodes rejected by the active environment surface as SKIPs in
 * the report rather than FAILs (e.g. FA(2) on DEMO/PROD per
 * {@code srodowiska.md}).
 *
 * <p><strong>Threading warning:</strong> {@code submitBatch} blocks the
 * calling thread for minutes to hours, depending on batch size and upload
 * bandwidth. KSeF batch can be up to 5 GB. Do not call from UI threads,
 * HTTP request handlers, or reactive framework dispatch threads. Wrap with
 * a dedicated executor for async use.
 *
 * <p>FULL mode only — sends actual invoices to KSeF.
 */
public final class BatchSessionRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchSessionRunner.class);
    private static final String NAME = "batchSession";
    private static final String OP_SUBMIT_BATCH = "submitBatch";
    private static final String SKIP_REASON_PREFIX = "FormCode rejected by environment: ";
    private static final String SKIP_REASON_PEF_AUTH =
            "FA_PEF/FA_KOR_PEF require Peppol provider auth (PefInvoiceWrite permission) — "
                    + "demo creds use regular NIP, KSeF returns 21405 \"kod formularza nie jest wspierany\". "
                    + "Wire-shape coverage for these FormCodes lives in WireMock contract tests.";
    private static final int INVOICE_COUNT = 3;

    private static final String HOST_FRAGMENT_TEST = "api-test.";
    private static final String HOST_FRAGMENT_DEMO = "api-demo.";
    private static final String HOST_PREFIX_PROD = "https://api.ksef.mf.gov.pl";

    private static final List<FormCode> FORM_CODES = List.of(
            FormCode.FA2, FormCode.FA3, FormCode.PEF3, FormCode.PEF_KOR3);

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
                results.add(RunResult.skip(NAME, OP_SUBMIT_BATCH + formCodeLabel,
                        SKIP_REASON_PREFIX + notAllowed.getMessage()));
                continue;
            }
            if (requiresPeppolProviderAuth(formCode)) {
                results.add(RunResult.skip(NAME, OP_SUBMIT_BATCH + formCodeLabel, SKIP_REASON_PEF_AUTH));
                continue;
            }
            runOnce(context, formCode, formCodeLabel, results);
        }
        return results;
    }

    private void runOnce(DemoContext context, FormCode formCode, String formCodeLabel,
                         List<RunResult> results) {
        List<Invoice> invoices = generateInvoices(formCode, context.nipIdentifier());

        long start = System.currentTimeMillis();
        try {
            BatchResult result = context.client().invoices()
                    .submitBatch(formCode, invoices, BatchOptions.defaults());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] {} submitted batch ref={}, total={}, cleared={}, failed={}",
                        NAME, formCodeLabel, result.sessionRef(), result.totalCount(),
                        result.successfulCount(), result.failedCount());
            }
            results.add(RunResult.ok(NAME, OP_SUBMIT_BATCH + formCodeLabel, elapsed(start),
                    "ref=" + result.sessionRef() + ", " + result.totalCount() + " invoices"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SUBMIT_BATCH + formCodeLabel, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private List<Invoice> generateInvoices(FormCode formCode, String sellerNip) {
        List<Invoice> invoices = new ArrayList<>(INVOICE_COUNT);
        for (int index = 0; index < INVOICE_COUNT; index++) {
            byte[] xml = TestInvoiceXml.generate(formCode, sellerNip);
            invoices.add(Invoice.fromXml(formCode, xml));
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
        if (envUrl.startsWith(HOST_PREFIX_PROD)) {
            return KsefEnvironment.PROD;
        }
        return KsefEnvironment.PROD;
    }
}
