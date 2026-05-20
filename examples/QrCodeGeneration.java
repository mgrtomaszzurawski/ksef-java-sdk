//DEPS io.github.mgrtomaszzurawski:ksef-client:0.1.0-preview
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example for the unofficial KSeF SDK (preview).
 * Not affiliated with Ministerstwo Finansow or CIRFMF.
 * API may change between 0.x releases; AGPL-3.0 warranty
 * disclaimer applies. For production use the official SDK:
 * https://github.com/CIRFMF/ksef-client-java
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Build a KSeF KOD I (online invoice) verification QR code. KOD I encodes
 *   a URL of the shape:
 *     https://qr-{env}.ksef.mf.gov.pl/invoice/{NIP}/{DD-MM-YYYY}/{base64UrlSha256}
 *   The QR contains the URL only; the visual label below the QR is either
 *   the KSeF number assigned by the system, or "OFFLINE" before assignment.
 *
 * Side effects on KSeF:
 *   None. Pure URL/QR construction — no API call, no authentication.
 *
 * Spec: ksef-docs/kody-qr.md, REQ-QR-04 .. REQ-QR-07.
 *
 * Inputs the snippet expects (positional args when run as-is):
 *   args[0] — invoice XML file path (used to compute SHA-256)
 *   args[1] — seller NIP (10 digits)
 *   args[2] — invoice issue date in ISO format YYYY-MM-DD
 *   args[3] — output PNG file path (e.g. qrcode.png)
 *   args[4] — KSeF number (35 chars) for the visual label (optional;
 *             absent → label "OFFLINE")
 *   KSEF_QR_ENV — TEST | DEMO | PROD env var (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Locale;

public final class QrCodeGeneration {

    private static final int MIN_REQUIRED_ARGS = 4;
    private static final int MAX_ALLOWED_ARGS = 5;
    private static final int ARG_INVOICE_PATH = 0;
    private static final int ARG_SELLER_NIP = 1;
    private static final int ARG_ISSUE_DATE = 2;
    private static final int ARG_OUTPUT_PATH = 3;
    private static final int ARG_KSEF_NUMBER = 4;
    private static final String SHA_256 = "SHA-256";
    private static final String ENV_VAR_NAME = "KSEF_QR_ENV";
    private static final String ENV_DEMO = "DEMO";
    private static final String ENV_PROD = "PROD";
    private static final String USAGE = "Usage: QrCodeGeneration <invoiceXml> <sellerNip>"
            + " <issueDateISO> <outputPng> [ksefNumber]";

    private QrCodeGeneration() { }

    public static void main(String[] args) throws Exception {
        if (args.length < MIN_REQUIRED_ARGS || args.length > MAX_ALLOWED_ARGS) {
            System.err.println(USAGE);
            System.exit(1);
        }
        Path invoicePath = Path.of(args[ARG_INVOICE_PATH]);
        String sellerNip = args[ARG_SELLER_NIP];
        LocalDate issueDate = LocalDate.parse(args[ARG_ISSUE_DATE]);
        Path outputPath = Path.of(args[ARG_OUTPUT_PATH]);
        String label = args.length == MAX_ALLOWED_ARGS
                ? args[ARG_KSEF_NUMBER]
                : QrCodeService.LABEL_OFFLINE;

        QrEnvironment qrEnv = resolveEnvironment(System.getenv(ENV_VAR_NAME));
        byte[] invoiceContent = Files.readAllBytes(invoicePath);
        byte[] invoiceSha256 = MessageDigest.getInstance(SHA_256).digest(invoiceContent);

        String verificationUrl = KsefVerificationLinks
                .buildInvoiceVerificationUrl(qrEnv, sellerNip, issueDate, invoiceSha256);
        System.out.println("Verification URL: " + verificationUrl);

        QrCodeService qrService = new QrCodeService();
        byte[] labeledPng = qrService.generateLabeledQrCode(verificationUrl, label);
        Files.write(outputPath, labeledPng);
        System.out.println("QR code written to " + outputPath
                + " (" + labeledPng.length + " bytes, label='" + label + "')");
    }

    private static QrEnvironment resolveEnvironment(String envName) {
        if (envName == null || envName.isBlank()) {
            return QrEnvironment.TEST;
        }
        String upper = envName.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "TEST" -> QrEnvironment.TEST;
            case ENV_DEMO -> QrEnvironment.DEMO;
            case ENV_PROD -> QrEnvironment.PROD;
            default -> throw new IllegalArgumentException(
                    "Unsupported KSEF_QR_ENV value: '" + envName
                    + "' (allowed: TEST, DEMO, PROD)");
        };
    }
}
