//DEPS io.github.mgrtomaszzurawski:ksef-client:0.1.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: generate a verification QR code (and the corresponding URL) for
 * a given KSeF number. Offline — no API call, no authentication.
 *
 * Required args (positional):
 *   args[0] — KSeF number (e.g. 1234567890-20260404-ABCDEF123456-78)
 *   args[1] — output PNG file path (e.g. qrcode.png)
 *
 * Optional env var:
 *   KSEF_ENV — TEST | DEMO | PREPROD | PROD (default: TEST). The QR code
 *              embeds an environment-specific verification URL.
 */
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QrCodeGeneration {

    private static final String ENV_TEST = "TEST";
    private static final String ENV_DEMO = "DEMO";
    private static final String ENV_LOWER_TEST = "test";
    private static final String ENV_LOWER_DEMO = "demo";

    private QrCodeGeneration() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: QrCodeGeneration <ksefNumber> <output.png>");
            System.exit(1);
        }
        String ksefNumber = args[0];
        Path outputPath = Path.of(args[1]);
        boolean isTestEnv = isTestEnvironment(System.getenv("KSEF_ENV"));

        QrCodeService service = new QrCodeService(isTestEnv);

        String verificationUrl = service.getVerificationUrl(ksefNumber);
        System.out.println("Verification URL: " + verificationUrl);

        byte[] pngBytes = service.generateQrCode(ksefNumber);
        Files.write(outputPath, pngBytes);
        System.out.println("QR code written to " + outputPath + " (" + pngBytes.length + " bytes)");
    }

    private static boolean isTestEnvironment(String envName) {
        if (envName == null || envName.isBlank()) {
            return true;
        }
        String upper = envName.toUpperCase();
        return upper.equals(ENV_TEST) || upper.equals(ENV_DEMO)
                || upper.contains(ENV_LOWER_TEST) || upper.contains(ENV_LOWER_DEMO);
    }
}
