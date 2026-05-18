/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.DemoMode;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.SelfSignedCerts;
import io.github.mgrtomaszzurawski.ksef.sample.util.TestInvoiceXml;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OfflineMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner exercising {@link OfflineInvoice} construction (PR14):
 * builder path, static-factory path, and (FULL only) the
 * {@code session.sendOfflineInvoice(...)} send path.
 *
 * <p>Probes use a fresh self-signed certificate generated in-memory.
 * KSeF would reject this on real submission — the offline-send probe
 * therefore expects (and tolerates) the server rejection and records
 * the exception class as the operation outcome rather than failing
 * the run.
 */
public final class OfflineInvoiceRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfflineInvoiceRunner.class);

    private static final String NAME = "offlineInvoice";
    private static final String OP_BUILDER = "offlineInvoiceBuilder";
    private static final String OP_STATIC_FACTORY = "offlineInvoiceFromInvoice";
    private static final String OP_SEND_OFFLINE = "sendOfflineInvoice";

    private static final String FAIL_NULL_KOD_I = "kodIQrPng() is null";
    private static final String FAIL_NULL_KOD_II = "kodIIQrPng() is null";
    private static final String FAIL_EMPTY_KOD_I = "kodIQrPng() is empty";
    private static final String FAIL_EMPTY_KOD_II = "kodIIQrPng() is empty";

    private static final String SKIP_NOT_FULL_MODE =
            "AUTH_SAFE — sendOfflineInvoice requires session-write capability";
    private static final String OK_REJECTED_BY_SERVER_PREFIX =
            "server rejected (expected — self-signed offline cert): ";
    /**
     * Per-field validation error — server returns this when the certificate
     * subject / signature chain cannot be matched to a registered entity.
     */
    private static final int EXPECTED_CODE_VALIDATION = 21405;
    /**
     * JSON-parsing error — server returns this when the encrypted offline
     * payload structure is rejected before per-field validation runs.
     */
    private static final int EXPECTED_CODE_JSON_PARSE = 21001;
    /** Substring the server may include when the rejection is certificate-related. */
    private static final String CERT_KEYWORD_EN = "certificate";
    /** Polish equivalent — production KSeF localises error messages. */
    private static final String CERT_KEYWORD_PL = "certyfikat";

    private static final String CERT_ORG = "KSeF Java SDK Demo Offline";
    private static final String CERT_ORG_ID_PREFIX = "VATPL-";
    private static final String CERT_CN_PREFIX = "OfflineDemo-";

    private static final String LOG_BUILT_VIA_BUILDER =
            "[{}] OfflineInvoice via builder: kodI={} bytes, kodII={} bytes, mode={}";
    private static final String LOG_BUILT_VIA_FACTORY =
            "[{}] OfflineInvoice via fromInvoice: kodI={} bytes, kodII={} bytes, mode={}";
    private static final String BYTES_SUFFIX = " bytes";
    private static final String KOD_LABEL_PREFIX = "kodI=";
    private static final String KOD_II_LABEL = ", kodII=";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        KsefCertificate testCertificate = generateTestCertificate(context.nipIdentifier());
        Invoice invoice = Invoice.fromXml(FormCode.FA3,
                TestInvoiceXml.generate(FormCode.FA3, context.nipIdentifier()));
        QrEnvironment qrEnvironment = QrEnvironment.fromKsefEnvironment(
                context.client().config().environment());
        LocalDate issueDate = LocalDate.now();

        OfflineInvoice<Invoice> viaBuilder = runBuilderProbe(invoice, testCertificate,
                qrEnvironment, context.nipIdentifier(), issueDate, results);
        OfflineInvoice<Invoice> viaFactory = runStaticFactoryProbe(invoice, testCertificate,
                qrEnvironment, context.nipIdentifier(), issueDate, results);

        runSendOfflineInvoiceProbe(context, viaBuilder != null ? viaBuilder : viaFactory, results);

        return results;
    }

    private OfflineInvoice<Invoice> runBuilderProbe(Invoice invoice, KsefCertificate certificate,
                                            QrEnvironment qrEnvironment, String sellerNip,
                                            LocalDate issueDate, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var offline = OfflineInvoiceBuilder.forInvoice(invoice)
                    .signingCertificate(certificate)
                    .offlineMode(OfflineMode.OFFLINE_24)
                    .qrEnvironment(qrEnvironment)
                    .contextType(QrContextType.NIP)
                    .contextValue(sellerNip)
                    .sellerNip(sellerNip)
                    .issueDate(issueDate)
                    .build();
            String message = assertQrPresent(OP_BUILDER, offline, results, start);
            if (message == null) {
                return null;
            }
            LOGGER.info(LOG_BUILT_VIA_BUILDER, NAME,
                    offline.kodIQrPng().length, offline.kodIIQrPng().length, offline.offlineMode());
            results.add(RunResult.ok(NAME, OP_BUILDER, elapsed(start), message));
            return offline;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_BUILDER, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private OfflineInvoice<Invoice> runStaticFactoryProbe(Invoice invoice, KsefCertificate certificate,
                                                  QrEnvironment qrEnvironment, String sellerNip,
                                                  LocalDate issueDate, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var offline = OfflineInvoice.fromInvoice(invoice, certificate, OfflineMode.OFFLINE_24,
                    new io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.OfflineSigningContext(
                            qrEnvironment, QrContextType.NIP, sellerNip, sellerNip, issueDate));
            String message = assertQrPresent(OP_STATIC_FACTORY, offline, results, start);
            if (message == null) {
                return null;
            }
            LOGGER.info(LOG_BUILT_VIA_FACTORY, NAME,
                    offline.kodIQrPng().length, offline.kodIIQrPng().length, offline.offlineMode());
            results.add(RunResult.ok(NAME, OP_STATIC_FACTORY, elapsed(start), message));
            return offline;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_STATIC_FACTORY, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    /**
     * Validate that both QR PNG byte arrays are present and non-empty.
     * Returns the OK message when valid, or {@code null} when a failure
     * has already been recorded into {@code results}.
     */
    private String assertQrPresent(String operation, OfflineInvoice<Invoice> offline,
                                   List<RunResult> results, long start) {
        byte[] kodI = offline.kodIQrPng();
        byte[] kodII = offline.kodIIQrPng();
        if (kodI == null) {
            results.add(RunResult.fail(NAME, operation, elapsed(start), FAIL_NULL_KOD_I));
            return null;
        }
        if (kodII == null) {
            results.add(RunResult.fail(NAME, operation, elapsed(start), FAIL_NULL_KOD_II));
            return null;
        }
        if (kodI.length == 0) {
            results.add(RunResult.fail(NAME, operation, elapsed(start), FAIL_EMPTY_KOD_I));
            return null;
        }
        if (kodII.length == 0) {
            results.add(RunResult.fail(NAME, operation, elapsed(start), FAIL_EMPTY_KOD_II));
            return null;
        }
        return KOD_LABEL_PREFIX + kodI.length + BYTES_SUFFIX
                + KOD_II_LABEL + kodII.length + BYTES_SUFFIX;
    }

    /**
     * FULL-only: open a fresh session and submit the offline invoice.
     * KSeF demo will reject the offline send (the cert is self-signed,
     * not a real KSeF Offline cert), so the probe records the rejection
     * as a successful observation rather than a runner failure.
     */
    private void runSendOfflineInvoiceProbe(DemoContext context, OfflineInvoice<Invoice> offline,
                                            List<RunResult> results) {
        if (context.mode() != DemoMode.FULL) {
            results.add(RunResult.skip(NAME, OP_SEND_OFFLINE, SKIP_NOT_FULL_MODE));
            return;
        }
        if (offline == null) {
            // Both build paths failed — already recorded as FAIL above. Skip the
            // send probe with a clear reason so the count stays consistent.
            results.add(RunResult.skip(NAME, OP_SEND_OFFLINE,
                    "no OfflineInvoice instance available — build probes failed"));
            return;
        }
        long start = System.currentTimeMillis();
        try (OnlineSession session = context.client().invoices().sessions().open(FormCode.FA3)) {
            var submitted = session.sendOfflineInvoice(offline);
            int statusCode = submitted.status().status() != null
                    ? submitted.status().status().code() : -1;
            results.add(RunResult.ok(NAME, OP_SEND_OFFLINE, elapsed(start),
                    "submitted ref=" + submitted.referenceNumber() + " status=" + statusCode));
        } catch (KsefException ksefException) {
            // Self-signed test certificate is expected to be rejected by the
            // server. Recognise the specific validation codes (21405 per-field
            // validation, 21001 JSON parse) and require the message to mention
            // 'certificate' (English) or 'certyfikat' (Polish — production
            // KSeF localises) — otherwise this is an unexpected error and the
            // probe must FAIL.
            Integer code = ksefException.exceptionCode();
            String body = ksefException.safeResponseBody();
            boolean expectedCode = code != null && (code == EXPECTED_CODE_VALIDATION
                    || code == EXPECTED_CODE_JSON_PARSE);
            String bodyLower = body == null ? "" : body.toLowerCase(Locale.ROOT);
            boolean mentionsCertificate = bodyLower.contains(CERT_KEYWORD_EN)
                    || bodyLower.contains(CERT_KEYWORD_PL);
            if (expectedCode && mentionsCertificate) {
                LOGGER.info("[{}] sendOfflineInvoice rejected with expected code {}", NAME, code);
                results.add(RunResult.ok(NAME, OP_SEND_OFFLINE, elapsed(start),
                        OK_REJECTED_BY_SERVER_PREFIX + "code=" + code));
            } else {
                results.add(RunResult.fail(NAME, OP_SEND_OFFLINE, elapsed(start),
                        "unexpected KsefException code=" + code + " body=" + body));
            }
        }
    }

    private static KsefCertificate generateTestCertificate(String sellerNip) {
        SelfSignedCerts.GeneratedCertificate generated = SelfSignedCerts.generate(
                CERT_ORG,
                CERT_ORG_ID_PREFIX + sellerNip,
                CERT_CN_PREFIX + sellerNip);
        return new KsefCertificate(generated.certificate(), generated.privateKey());
    }
}
