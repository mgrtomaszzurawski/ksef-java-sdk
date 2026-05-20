//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Request a new KSeF certificate via the SDK's workflow wrapper, then
 *   revoke it. The single requestNewCertificate(keyPair) call pulls
 *   limits, fetches the required CSR subject, builds the CSR locally,
 *   submits enroll, polls until terminal, and retrieves the DER bytes
 *   — see ADR-032 (sync-default operation pattern).
 *   Authentication uses XAdES because cert-management endpoints reject
 *   token-only auth.
 *
 * Side effects on KSeF:
 *   Burns one cert quota slot per run (max 12/month, max 6 active per taxpayer).
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_P12_FILE     — path to PKCS#12 keystore
 *   KSEF_P12_PASSWORD — keystore password
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public final class EnrollAndRevokeCertificate {

    private static final int RSA_KEY_SIZE = 2048;

    private EnrollAndRevokeCertificate() { }

    public static void main(String[] args) throws Exception {
        Path p12Path = Path.of(requireEnv("KSEF_P12_FILE"));
        char[] p12Password = requireEnv("KSEF_P12_PASSWORD").toCharArray();
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefPkcs12Credentials(p12Path, p12Password, nip))
                .build()) {

            // Drive lazy auth via any authenticated read so the PKCS#12
            // material is consumed up-front; then zero the password buffer
            // so a heap dump cannot recover it.
            client.authSessions().streamAuthSessions(
                    io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model.AuthSessionsQueryRequest.defaults()).findAny();
            java.util.Arrays.fill(p12Password, '\0');
            System.out.println("XAdES authenticated as ***" + nip.substring(Math.max(0, nip.length() - 4)));

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            RetrievedCertificate cert = client.certificates().requestNewCertificate(keyPair);
            System.out.println("Certificate issued, serial: " + cert.certificateSerialNumber());

            client.certificates().revoke(cert.certificateSerialNumber(), CertificateRevocationReason.UNSPECIFIED);
            System.out.println("Certificate revoked");
        } finally {
            java.util.Arrays.fill(p12Password, '\0');
        }
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
