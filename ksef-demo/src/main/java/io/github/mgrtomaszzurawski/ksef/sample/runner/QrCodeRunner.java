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
    private static final String SAMPLE_SELLER_NIP = "1234567890";
    private static final String SAMPLE_INVOICE_XML = "<Faktura>fixture</Faktura>";
    private static final String SHA_256 = "SHA-256";
    private static final LocalDate SAMPLE_ISSUE_DATE = LocalDate.of(2026, 4, 4);
    private static final String ENV_DEMO = "demo";
    private static final String ENV_TEST = "test";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        boolean isTestEnv = context.environment().contains(ENV_DEMO) || context.environment().contains(ENV_TEST);
        QrEnvironment qrEnv = isTestEnv ? QrEnvironment.TEST : QrEnvironment.PROD;
        QrCodeService service = new QrCodeService();

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

        return results;
    }
}
