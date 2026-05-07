//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Enroll a new certificate from a CSR, poll until the serial appears, then
 *   revoke. Authentication uses XAdES because cert-management endpoints reject
 *   token-only auth.
 *
 * Side effects on KSeF:
 *   Burns one cert quota slot per run (max 12/month, max 6 active per taxpayer).
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_P12_FILE     — path to PKCS#12 keystore
 *   KSEF_P12_PASSWORD — keystore password
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_CSR_DER      — path to a PKCS#10 CSR in DER format
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EnrollAndRevokeCertificate {

    private static final long POLL_INTERVAL_MS = 2000;
    private static final long POLL_TIMEOUT_MS = 300_000; // 5 minutes

    private EnrollAndRevokeCertificate() { }

    public static void main(String[] args) throws Exception {
        Path p12Path = Path.of(requireEnv("KSEF_P12_FILE"));
        char[] p12Password = requireEnv("KSEF_P12_PASSWORD").toCharArray();
        String nip = requireEnv("KSEF_NIP");
        Path csrPath = Path.of(requireEnv("KSEF_CSR_DER"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        byte[] csrDer = Files.readAllBytes(csrPath);

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefPkcs12Credentials(p12Path, p12Password, nip))
                .build()) {

            client.authenticate();
            // After authentication the password buffer is no longer
            // needed; zero it so a heap dump cannot recover it.
            java.util.Arrays.fill(p12Password, '\0');
            System.out.println("XAdES authenticated as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            CertificateEnrollBuilder enrollBuilder = CertificateEnrollBuilder
                    .create("Example certificate", KsefCertificateType.AUTHENTICATION, csrDer);

            EnrollCertificateResult enroll = client.certificates().enroll(enrollBuilder);
            System.out.println("Enrollment submitted, ref: " + enroll.referenceNumber());

            String serial = pollForSerial(client, enroll.referenceNumber());
            if (serial == null) {
                System.out.println("Serial number never appeared within timeout — leaving as-is");
                return;
            }
            System.out.println("Certificate issued, serial: " + serial);

            client.certificates().revoke(serial);
            System.out.println("Certificate revoked");
        } finally {
            java.util.Arrays.fill(p12Password, '\0');
        }
    }

    private static String pollForSerial(KsefClient client, String referenceNumber) throws InterruptedException {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            CertificateEnrollmentStatus status = client.certificates().getEnrollmentStatus(referenceNumber);
            if (status.certificateSerialNumber() != null) {
                return status.certificateSerialNumber();
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return null;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String envName) {
        if (envName == null || envName.isBlank()) {
            return KsefEnvironment.TEST;
        }
        return switch (envName.toUpperCase()) {
            case "TEST" -> KsefEnvironment.TEST;
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
