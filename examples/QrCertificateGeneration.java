//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Build a KSeF KOD II (offline-certificate) verification QR code. KOD II is
 *   used when the consumer has a KSeF Offline certificate and needs to encode
 *   a self-verifying QR — the URL carries the seller's signature over the
 *   path components, so the verifier can confirm authenticity without
 *   contacting KSeF online.
 *
 *   URL shape:
 *     https://qr-{env}.ksef.mf.gov.pl/certificate/{contextType}/{contextValue}
 *       /{sellerNip}/{certificateSerial}/{base64UrlSha256}/{base64UrlSignature}
 *
 *   Two flows are documented (ADR-019):
 *     1. Owns-key flow — SDK has the PrivateKey and signs internally:
 *          QrSigningService.certificateVerificationUrl(env, input, privateKey)
 *     2. PKI-neutral flow — consumer signs externally (HSM, custom signer):
 *          KsefVerificationLinks.canonicalCertificateSigningPayload(env, input)
 *          → consumer signs payload with their own crypto stack
 *          → KsefVerificationLinks.buildCertificateVerificationUrl(env, paramsWithSig)
 *
 *   This example uses the owns-key flow with a PKCS#12 keystore. Algorithm
 *   is auto-detected from key type: RSA → RSASSA-PSS, EC P-256 → ECDSA in
 *   IEEE P1363 fixed-length format.
 *
 * Side effects on KSeF:
 *   None. Pure URL/QR construction — no API call, no authentication.
 *
 * Spec: ksef-docs/kody-qr.md, ADR-019 (KOD II signing scheme).
 *
 * Inputs the snippet expects (positional args when run as-is):
 *   args[0] — PKCS#12 keystore path (KSeF Offline certificate)
 *   args[1] — keystore password
 *   args[2] — seller NIP (10 digits) — must match cert subject
 *   args[3] — certificate serial number (KSeF-assigned, from enrollment)
 *   args[4] — invoice XML file path (used to compute SHA-256)
 *   args[5] — output PNG file path (e.g. kod2.png)
 *   KSEF_QR_ENV — TEST | DEMO | PROD env var (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrCodeService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrSigningService;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;

public final class QrCertificateGeneration {

    private static final int MIN_REQUIRED_ARGS = 6;
    private static final int ARG_KEYSTORE_PATH = 0;
    private static final int ARG_KEYSTORE_PASSWORD = 1;
    private static final int ARG_SELLER_NIP = 2;
    private static final int ARG_CERTIFICATE_SERIAL = 3;
    private static final int ARG_INVOICE_PATH = 4;
    private static final int ARG_OUTPUT_PATH = 5;
    private static final String SHA_256 = "SHA-256";
    private static final String ENV_VAR_NAME = "KSEF_QR_ENV";
    private static final String DEFAULT_ENV = "TEST";
    private static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";

    private QrCertificateGeneration() { }

    public static void main(String[] args) throws Exception {
        if (args.length < MIN_REQUIRED_ARGS) {
            System.err.println("Usage: QrCertificateGeneration <p12> <password> <sellerNip> <certSerial> <invoiceXml> <output.png>");
            System.exit(1);
        }
        Path keystorePath = Path.of(args[ARG_KEYSTORE_PATH]);
        char[] keystorePassword = args[ARG_KEYSTORE_PASSWORD].toCharArray();
        String sellerNip = args[ARG_SELLER_NIP];
        String certificateSerial = args[ARG_CERTIFICATE_SERIAL];
        Path invoicePath = Path.of(args[ARG_INVOICE_PATH]);
        Path outputPath = Path.of(args[ARG_OUTPUT_PATH]);
        QrEnvironment environment = resolveEnv();

        // Load the offline certificate's private key from PKCS#12.
        KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE_PKCS12);
        try (InputStream in = Files.newInputStream(keystorePath)) {
            keystore.load(in, keystorePassword);
        }
        String alias = firstAlias(keystore);
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, keystorePassword);
        X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
        // Zero out the password buffer once it's no longer needed.
        java.util.Arrays.fill(keystorePassword, '\0');

        // Compute SHA-256 of the invoice XML.
        byte[] invoiceXml = Files.readAllBytes(invoicePath);
        byte[] invoiceSha256 = MessageDigest.getInstance(SHA_256).digest(invoiceXml);

        // Build the canonical signing input (no signature yet).
        KsefVerificationLinks.CertificateSigningInput input =
                new KsefVerificationLinks.CertificateSigningInput(
                        QrContextType.NIP,
                        sellerNip,
                        sellerNip,
                        certificateSerial,
                        invoiceSha256);

        // Owns-key flow: SDK signs internally + builds full URL.
        QrSigningService signer = new QrSigningService();
        String url = signer.certificateVerificationUrl(environment, input, privateKey);

        System.out.println("KOD II URL: " + url);
        System.out.println("Cert subject: " + cert.getSubjectX500Principal());
        System.out.println("Algorithm: " + privateKey.getAlgorithm());

        // Render the URL as a QR PNG with the cert serial as visual label.
        QrCodeService qrService = new QrCodeService();
        byte[] png = qrService.addLabelToQrCode(qrService.generateQrCode(url), certificateSerial);
        Files.write(outputPath, png);
        System.out.println("QR written to " + outputPath + " (" + png.length + " bytes)");
    }

    private static QrEnvironment resolveEnv() {
        String envName = System.getenv().getOrDefault(ENV_VAR_NAME, DEFAULT_ENV);
        return switch (envName.toUpperCase(Locale.ROOT)) {
            case "TEST" -> QrEnvironment.TEST;
            case "DEMO" -> QrEnvironment.DEMO;
            case "PROD" -> QrEnvironment.PROD;
            default -> throw new IllegalArgumentException("Unknown " + ENV_VAR_NAME + ": " + envName);
        };
    }

    private static String firstAlias(KeyStore keystore) throws java.security.KeyStoreException {
        Enumeration<String> aliases = keystore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalStateException("Keystore is empty");
        }
        return aliases.nextElement();
    }
}
