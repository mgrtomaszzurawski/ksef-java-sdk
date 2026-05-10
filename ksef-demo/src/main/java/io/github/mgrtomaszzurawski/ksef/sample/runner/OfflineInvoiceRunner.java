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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SubmittedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
                context.client().environment());
        LocalDate issueDate = LocalDate.now();

        OfflineInvoice viaBuilder = runBuilderProbe(invoice, testCertificate,
                qrEnvironment, context.nipIdentifier(), issueDate, results);
        OfflineInvoice viaFactory = runStaticFactoryProbe(invoice, testCertificate,
                qrEnvironment, context.nipIdentifier(), issueDate, results);

        runSendOfflineInvoiceProbe(context, viaBuilder != null ? viaBuilder : viaFactory, results);

        return results;
    }

    private OfflineInvoice runBuilderProbe(Invoice invoice, KsefCertificate certificate,
                                            QrEnvironment qrEnvironment, String sellerNip,
                                            LocalDate issueDate, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OfflineInvoice offline = OfflineInvoiceBuilder.forInvoice(invoice)
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

    private OfflineInvoice runStaticFactoryProbe(Invoice invoice, KsefCertificate certificate,
                                                  QrEnvironment qrEnvironment, String sellerNip,
                                                  LocalDate issueDate, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            OfflineInvoice offline = OfflineInvoice.fromInvoice(
                    invoice, certificate, OfflineMode.OFFLINE_24,
                    qrEnvironment, QrContextType.NIP, sellerNip,
                    sellerNip, issueDate);
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
    private String assertQrPresent(String operation, OfflineInvoice offline,
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
    private void runSendOfflineInvoiceProbe(DemoContext context, OfflineInvoice offline,
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
        try (OnlineSession session = context.client().invoices().openSession(FormCode.FA3)) {
            SubmittedInvoice submitted = session.sendOfflineInvoice(offline);
            int statusCode = submitted.status().status() != null
                    ? submitted.status().status().code() : -1;
            results.add(RunResult.ok(NAME, OP_SEND_OFFLINE, elapsed(start),
                    "submitted ref=" + submitted.referenceNumber() + " status=" + statusCode));
        } catch (Exception exception) {
            String rejectedClass = exception.getClass().getSimpleName();
            LOGGER.info("[{}] sendOfflineInvoice rejected: {}", NAME, rejectedClass);
            results.add(RunResult.ok(NAME, OP_SEND_OFFLINE, elapsed(start),
                    OK_REJECTED_BY_SERVER_PREFIX + rejectedClass));
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
