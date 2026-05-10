/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for QrCodeService operations. Offline — no API calls.
 */
public final class QrCodeRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(QrCodeRunner.class);
    private static final String NAME = "qrcode";
    private static final String OP_URL = "buildInvoiceVerificationUrl";
    private static final String OP_QR = "generateQrCode";
    private static final String OP_CLIENT_ACCESSOR = "clientQrCodeAccessor";
    private static final String SAMPLE_SELLER_NIP = "1111111111";
    private static final String SAMPLE_INVOICE_XML = "<Faktura>fixture</Faktura>";
    private static final String SHA_256 = "SHA-256";
    private static final LocalDate SAMPLE_ISSUE_DATE = LocalDate.of(2026, 4, 4);
    private static final String ENV_DEMO = "demo";
    private static final String ENV_TEST = "test";
    private static final String FAIL_NULL_QR_CODE_ACCESSOR =
            "client.qrCode() returned null — KsefClient accessor (PR21) wiring broken";
    private static final String FAIL_EMPTY_PNG = "PNG bytes are empty";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        boolean isTestEnv = context.environment().contains(ENV_DEMO) || context.environment().contains(ENV_TEST);
        QrEnvironment qrEnv = isTestEnv ? QrEnvironment.TEST : QrEnvironment.PROD;
        QrCodeService service = context.client().qrCode();

        long start = System.currentTimeMillis();
        String verificationUrl = null;
        try {
            byte[] hash = MessageDigest.getInstance(SHA_256)
                    .digest(SAMPLE_INVOICE_XML.getBytes(StandardCharsets.UTF_8));
            verificationUrl = KsefVerificationLinks.buildInvoiceVerificationUrl(
                    qrEnv, SAMPLE_SELLER_NIP, SAMPLE_ISSUE_DATE, hash);
            LOGGER.info("[{}] verification URL: {}", NAME, verificationUrl);
            results.add(RunResult.ok(NAME, OP_URL, elapsed(start), verificationUrl));
        } catch (NoSuchAlgorithmException | RuntimeException exception) {
            results.add(RunResult.fail(NAME, OP_URL, elapsed(start), errorMessage(exception)));
        }

        if (verificationUrl != null) {
            start = System.currentTimeMillis();
            try {
                byte[] pngBytes = service.generateQrCode(verificationUrl);
                LOGGER.info("[{}] QR code generated: {} bytes PNG", NAME, pngBytes.length);
                results.add(RunResult.ok(NAME, OP_QR, elapsed(start), pngBytes.length + " bytes"));
            } catch (RuntimeException exception) {
                results.add(RunResult.fail(NAME, OP_QR, elapsed(start), errorMessage(exception)));
            }
        }

        runClientQrCodeAccessor(context, qrEnv, results);

        return results;
    }

    /**
     * Verify the {@code KsefClient.qrCode()} accessor (PR21) returns a
     * non-null {@link QrCodeService} that can render a real KSeF
     * verification URL into a non-empty PNG.
     */
    private void runClientQrCodeAccessor(DemoContext context, QrEnvironment qrEnv,
                                         List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            QrCodeService accessorService = context.client().qrCode();
            if (accessorService == null) {
                results.add(RunResult.fail(NAME, OP_CLIENT_ACCESSOR, elapsed(start),
                        FAIL_NULL_QR_CODE_ACCESSOR));
                return;
            }
            byte[] hash = MessageDigest.getInstance(SHA_256)
                    .digest(SAMPLE_INVOICE_XML.getBytes(StandardCharsets.UTF_8));
            String payloadUrl = KsefVerificationLinks.buildInvoiceVerificationUrl(
                    qrEnv, SAMPLE_SELLER_NIP, SAMPLE_ISSUE_DATE, hash);
            byte[] pngBytes = accessorService.generateQrCode(payloadUrl);
            if (pngBytes == null || pngBytes.length == 0) {
                results.add(RunResult.fail(NAME, OP_CLIENT_ACCESSOR, elapsed(start), FAIL_EMPTY_PNG));
                return;
            }
            LOGGER.info("[{}] client.qrCode() rendered {} bytes PNG (url len={})",
                    NAME, pngBytes.length, payloadUrl.length());
            results.add(RunResult.ok(NAME, OP_CLIENT_ACCESSOR, elapsed(start),
                    pngBytes.length + " bytes (url=" + payloadUrl.length() + " chars)"));
        } catch (NoSuchAlgorithmException | RuntimeException exception) {
            results.add(RunResult.fail(NAME, OP_CLIENT_ACCESSOR, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
