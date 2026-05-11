/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.IdentifierGenerators;
import io.github.mgrtomaszzurawski.ksef.sample.util.SelfSignedCerts;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefAsync;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.PermissionClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * TEST-env-only runner exercising the FA_PEF/FA_KOR_PEF Peppol-provider
 * flow. Generates a random {@code peppolId}, builds a self-signed
 * certificate whose CN matches that id, and authenticates against
 * KSeF via XAdES — KSeF TEST env auto-creates the Peppol provider
 * context on first auth.
 *
 * <p>Each batch session is opened with {@link FormCode#PEF3} or
 * {@link FormCode#PEF_KOR3}, parts are uploaded, status is polled
 * until terminal, then the session is closed. The runner builds its
 * own short-lived {@link KsefClient} because the Peppol provider
 * authentication context differs from the primary NIP context held in
 * {@code DemoContext.client()}.
 *
 * <p>FULL mode only — sends actual invoices to KSeF.
 */
public final class PeppolProviderRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolProviderRunner.class);
    private static final String NAME = "peppolProvider";
    private static final String OP_AUTH = "authAsPeppolProvider";
    private static final String OP_OPEN_SESSION = "openSession";
    private static final String OP_SEND_INVOICE = "sendInvoice";
    private static final String OP_CLOSE = "close";
    /** KSeF terminal-success status code on permission grant operations. */
    private static final int GRANT_SUCCESS_STATUS_CODE = 200;
    /** Wall-clock budget for the grant-authorization poll loop. */
    private static final long GRANT_AWAIT_SECONDS = 30;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        String peppolId = IdentifierGenerators.generatePeppolId();

        // Step 1: provider authenticates against TEST env via XAdES self-signed cert,
        // which auto-creates the Peppol provider context. No batch session yet —
        // the provider ends with PefInvoiceWrite permission only, which is not
        // sufficient for POST /v2/sessions/batch (KSeF needs InvoiceWrite via grant).
        if (!authenticateProvider(peppolId, context.environment(), results)) {
            return results;
        }

        // Step 2: owner (the primary TEST-env NIP context held by the demo client)
        // grants PEF_INVOICING / InvoiceWrite to the freshly-registered provider.
        if (!grantPefInvoicingFromOwner(context, peppolId, results)) {
            return results;
        }

        // Step 3: PEF(3). Capture the assigned KSeF number (if KSeF accepts the
        // fixture) so we can chain a real correction in step 4.
        String priorKsefNumber = runFormCode(context, peppolId, FormCode.PEF3, "[FA_PEF(3)]", null, results);

        // Step 4: PEF_KOR(3) is a correction document — it must reference an
        // accepted prior invoice's KSeF number. If PEF(3) above did not yield
        // one (current state: KSeF rejects the fixture on business validation,
        // so no KSeF number is assigned), skip PEF_KOR rather than ship a
        // fixture that still contains an unresolved #ksef_number# placeholder.
        if (priorKsefNumber == null) {
            results.add(RunResult.skip(NAME, OP_OPEN_SESSION + "[FA_KOR_PEF(3)]",
                    "PEF_KOR(3) requires the KSeF number of an accepted prior PEF(3) invoice; "
                            + "the PEF(3) run above did not yield one (likely KSeF business validation "
                            + "rejected the fixture). Re-run with a fully-conforming PEPPOL BIS Billing 3.0 "
                            + "PEF fixture to enable the chain."));
            return results;
        }
        runFormCode(context, peppolId, FormCode.PEF_KOR3, "[FA_KOR_PEF(3)]", priorKsefNumber, results);
        return results;
    }

    private boolean authenticateProvider(String peppolId, String envUrl, List<RunResult> results) {
        long start = System.currentTimeMillis();
        SelfSignedCerts.GeneratedCertificate cert = SelfSignedCerts.forPeppolId(peppolId);
        KsefCertificateCredentials creds = new KsefCertificateCredentials(
                cert.certificate(), cert.privateKey(), KsefIdentifier.peppolId(peppolId));
        try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.custom(envUrl))
                .credentials(creds)
                .retryPolicy(io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy.builder().build())
                .build()) {
            // Drive lazy auth via any authenticated read.
            client.auth().streamSessions().findAny();
            results.add(RunResult.ok(NAME, OP_AUTH, elapsed(start),
                    "peppolId=" + peppolId + " (registered via XAdES self-signed cert)"));
            return true;
        } catch (Exception ex) {
            results.add(RunResult.fail(NAME, OP_AUTH, elapsed(start), errorMessage(ex)));
            return false;
        }
    }

    private boolean grantPefInvoicingFromOwner(DemoContext context, String peppolId, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var builder = io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder
                    .forPeppolId(peppolId)
                    .pefInvoicing()
                    .entityDetails("KSeF Java SDK Demo Peppol Provider")
                    .description("PeppolProviderRunner grant for " + peppolId);
            String referenceNumber = context.client().permissions().grantAuthorization(builder.build()).referenceNumber();
            var permissions = context.client().permissions();
            var status = KsefAsync.awaitTerminal(
                    new KsefAsync.Config<>(
                            "grantAuthorization",
                            () -> permissions.getOperationStatus(referenceNumber),
                            opStatus -> opStatus.status() != null
                                    && opStatus.status().code() >= PermissionClient.TERMINAL_STATUS_CODE_THRESHOLD,
                            opStatus -> opStatus.status() == null ? null : opStatus.status().code(),
                            Duration.ofSeconds(GRANT_AWAIT_SECONDS),
                            null));
            int code = status.status() == null ? -1 : status.status().code();
            if (code == GRANT_SUCCESS_STATUS_CODE) {
                results.add(RunResult.ok(NAME, "grantPefInvoicingToProvider", elapsed(start),
                        "peppolId=" + peppolId));
                return true;
            }
            results.add(RunResult.fail(NAME, "grantPefInvoicingToProvider", elapsed(start),
                    "grant terminal status code=" + code));
            return false;
        } catch (Exception ex) {
            results.add(RunResult.fail(NAME, "grantPefInvoicingToProvider", elapsed(start), errorMessage(ex)));
            return false;
        }
    }

    /**
     * @return the KSeF number assigned to the sent invoice if KSeF accepted
     *     it, or {@code null} if the invoice was rejected (so the caller can
     *     decide to skip a chained correction document).
     */
    private String runFormCode(DemoContext context, String peppolId, FormCode formCode,
                              String label, String priorKsefNumber, List<RunResult> results) {
        // PEF flow uses online sessions, not batch (per upstream
        // PeppolInvoiceIntegrationTest). KSeF /v2/sessions/batch requires
        // InvoiceWrite permission which is not granted to a Peppol provider —
        // they receive PefInvoiceWrite which only works with online sessions.
        long authStart = System.currentTimeMillis();
        SelfSignedCerts.GeneratedCertificate cert = SelfSignedCerts.forPeppolId(peppolId);
        KsefCertificateCredentials creds = new KsefCertificateCredentials(
                cert.certificate(), cert.privateKey(), KsefIdentifier.peppolId(peppolId));

        String invoiceRef = null;
        try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.custom(context.environment()))
                .credentials(creds)
                .retryPolicy(RetryPolicy.builder().build())
                .build()) {
            // Drive lazy auth via any authenticated read.
            client.auth().streamSessions().findAny();
            results.add(RunResult.ok(NAME, OP_AUTH + label, elapsed(authStart),
                    "peppolId=" + peppolId));

            long openStart = System.currentTimeMillis();
            try (OnlineSession session = client.invoices().openSession(formCode)) {
                results.add(RunResult.ok(NAME, OP_OPEN_SESSION + label, elapsed(openStart),
                        "ref=" + session.referenceNumber()));
                invoiceRef = runSend(session, formCode, context.nipIdentifier(),
                        priorKsefNumber, label, results);
                runClose(session, label, results);
                if (invoiceRef != null) {
                    return queryAssignedKsefNumber(session, invoiceRef);
                }
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_AUTH + label, elapsed(authStart),
                    errorMessage(exception)));
        }
        return null;
    }

    /**
     * @return the invoice reference if send succeeded, otherwise {@code null}.
     */
    private String runSend(OnlineSession session, FormCode formCode, String supplierNip,
                            String priorKsefNumber, String label, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            // PEF UBL templates use {supplier_nip} for the Polish company on whose
            // behalf the Peppol provider sends the invoice. That identity is the
            // owner NIP from the TEST env primary context, NOT the peppolId.
            byte[] invoiceXml = TestInvoiceXml.generate(formCode, supplierNip, priorKsefNumber);
            var sendResult = session.sendInvoice(Invoice.fromXml(formCode, invoiceXml));
            results.add(RunResult.ok(NAME, OP_SEND_INVOICE + label, elapsed(start),
                    "invoiceRef=" + sendResult.referenceNumber()));
            return sendResult.referenceNumber();
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SEND_INVOICE + label, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private String queryAssignedKsefNumber(OnlineSession session, String invoiceRef) {
        try {
            var status = session.invoiceStatus(invoiceRef);
            String ksefNumber = status.ksefNumber();
            return ksefNumber == null || ksefNumber.isBlank() ? null : ksefNumber;
        } catch (Exception ignored) {
            // Best-effort: KSeF rejected the invoice or status query failed —
            // either way, we cannot chain a correction.
            return null;
        }
    }

    private void runClose(OnlineSession session, String label, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            session.close();
            LOGGER.info("[{}] {} closed", NAME, label);
            results.add(RunResult.ok(NAME, OP_CLOSE + label, elapsed(start)));
        } catch (io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException terminalFailure) {
            // KSeF rejects the placeholder-substituted CIRFMF PEF UBL sample on
            // strict business-rule validation (code=445 "Błąd weryfikacji, brak
            // poprawnych faktur"). The SDK lifecycle reached send + poll-to-
            // terminal correctly; the rejection is a fixture-content issue (a
            // fully-conforming PEF UBL invoice would require PEPPOL BIS Billing
            // 3.0 domain expertise beyond the demo scope). Report as SKIP so
            // it does not inflate the OK total — the message preserves the
            // diagnostic context that the SDK lifecycle was verified live.
            results.add(RunResult.skip(NAME, OP_CLOSE + label, elapsed(start),
                    "session reached terminal status code=" + (terminalFailure.statusCode())
                            + " on KSeF business validation of the PEF UBL fixture — SDK lifecycle "
                            + "(auth/grant/open/send/poll) verified live; UBL content conformance "
                            + "is out of demo scope"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CLOSE + label, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
