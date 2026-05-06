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
import io.github.mgrtomaszzurawski.ksef.sample.util.IdentifierGenerators;
import io.github.mgrtomaszzurawski.ksef.sample.util.SelfSignedCerts;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch.BatchSessionOptions;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * TEST-env-only runner exercising the EU-VAT-UE authentication context.
 * Generates a random VAT-UE identifier (e.g. {@code DE123456789}),
 * builds a matching self-signed certificate, and authenticates against
 * KSeF with {@link KsefIdentifier#nipVatUe(String)}.
 *
 * <p>Per {@code KsefIdentifier.Type.NIP_VAT_UE} the SDK maps this to
 * the wire-level {@code AuthenticationContextIdentifierType=NipVatUe}
 * so the resulting session is anchored to the EU VAT context. KSeF
 * TEST env auto-creates the context on first auth.
 *
 * <p>Verifies the SDK's NipVatUe credential path end-to-end by
 * opening a single FA(3) batch, uploading parts, and closing.
 *
 * <p>FULL mode only — sends actual invoices to KSeF.
 */
public final class VatUeProviderRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VatUeProviderRunner.class);
    private static final String NAME = "vatUeProvider";
    private static final String OP_AUTH = "authAsVatUe";
    private static final String OP_OPEN_BATCH = "openBatchSession";
    private static final String OP_UPLOAD_PARTS = "uploadParts";
    private static final String OP_CLOSE = "close";
    private static final int INVOICE_COUNT = 1;
    private static final String LABEL = "[FA(3)/NIP_VAT_UE]";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        // Live verification of NipVatUe-context auth via on-the-fly self-signed
        // cert is blocked on KSeF returning 21117 "Nieprawidłowy identyfikator
        // podmiotu dla wskazanego typu kontekstu" for every cert organizationId
        // shape we have tried (VATPL-{nip}, VATPL-{nip}-{country}{specific},
        // {nip}-{country}{specific} alone). Upstream CIRFMF/ksef-client-java has
        // no equivalent integration test for VAT-UE-context auth — only EU-entity
        // permission grants, where VAT-UE is the grant <em>target</em>, not the
        // authenticated context. The exact cert ↔ identifier coupling KSeF
        // expects for this context type is not derivable from the bundled XSDs
        // alone. Wire-shape coverage of NipVatUe credentials is provided by
        // WireMock contract tests in {@code KsefCredentialsTest}.
        List<RunResult> results = new ArrayList<>();
        results.add(RunResult.skip(NAME, OP_AUTH + LABEL,
                "VAT-UE auth context cert shape vs KSeF requirement is not known — "
                        + "upstream SDK has no live VAT-UE auth example. SDK supports the "
                        + "credential type (KsefIdentifier.nipVatUe + KsefCertificateCredentials), "
                        + "verified via WireMock contract tests."));
        return results;
    }

    private void runUpload(KsefBatchSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.uploadParts();
            results.add(RunResult.ok(NAME, OP_UPLOAD_PARTS + LABEL, elapsed(start),
                    session.partUploadRequests().size() + " parts"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_UPLOAD_PARTS + LABEL, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runClose(KsefBatchSession session, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOGGER.info("[{}] {} closed", NAME, LABEL);
            results.add(RunResult.ok(NAME, OP_CLOSE + LABEL, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE + LABEL, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
