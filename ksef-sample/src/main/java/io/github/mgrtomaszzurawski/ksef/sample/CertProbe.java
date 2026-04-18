/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
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
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.model.AuthenticationStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.CertificateQueryBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Comparator;
import java.util.List;

/**
 * Standalone probe to verify two RCAs:
 * (1) RCA-cert-enroll-async-serial-2026-04-04-1750 — does the serial number
 *     ever populate after enrollment?
 * (2) RCA-cert-ops-require-cert-auth-2026-04-04-1718 — does canRequest=false
 *     reflect quota exhaustion (remaining=0) or auth method?
 *
 * Steps:
 *   1. XAdES auth using cert from p12
 *   2. Read /certificates/limits (canRequest + remaining/limit BEFORE)
 *   3. Query active certs, identify youngest by validFrom
 *   4. Revoke youngest
 *   5. Read /certificates/limits AFTER revoke
 *   6. Enroll a new cert (CSR generation)
 *   7. Poll enrollment status until serial appears or timeout
 *   8. Read /certificates/limits AFTER enroll
 *   9. If serial appeared, revoke the new cert too (cleanup)
 */
public final class CertProbe {

    private static final Logger LOG = LoggerFactory.getLogger(CertProbe.class);
    private static final Path CREDENTIALS_FILE = Path.of("ksef-credentials.properties");
    private static final String CERT_NAME = "Probe Cert RCA Verify";
    private static final String STATUS_ACTIVE = "Active";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String DN_CN_PREFIX = "CN=";
    private static final String DN_COUNTRY_PREFIX = ",C=";
    private static final String DN_GIVEN_NAME_PREFIX = ",GIVENNAME=";
    private static final String DN_SURNAME_PREFIX = ",SURNAME=";
    private static final String DN_SERIAL_NUMBER_PREFIX = ",SERIALNUMBER=";
    private static final String DN_ORGANIZATION_PREFIX = ",O=";
    private static final String OID_ORGANIZATION_IDENTIFIER = "2.5.4.97";
    private static final String DN_ORGANIZATION_IDENTIFIER_PREFIX = "," + OID_ORGANIZATION_IDENTIFIER + "=";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int AUTH_STATUS_OK = 200;
    private static final int POLL_INITIAL_DELAY_MS = 1000;
    private static final int POLL_MAX_DELAY_MS = 10000;
    private static final int AUTH_POLL_TIMEOUT_MS = 60000;
    private static final int ENROLL_POLL_TIMEOUT_MS = 300000; // 5 minutes
    private static final int POLL_BACKOFF_MULTIPLIER = 2;
    private static final int EXIT_FAILURE = 1;
    private static final String SEPARATOR = "================================================================";

    private CertProbe() { }

    public static void main(String[] args) {
        AppProperties properties = AppProperties.load(CREDENTIALS_FILE);
        if (!properties.hasCertificate()) {
            LOG.error("No certificate configured in {}", CREDENTIALS_FILE);
            System.exit(EXIT_FAILURE);
        }

        KsefPkcs12Credentials credentials = new KsefPkcs12Credentials(
                Path.of(properties.certFile()),
                properties.certPassword().toCharArray(),
                properties.nipIdentifier());

        try (KsefClient client = KsefClient.builder(KsefEnvironment.custom(properties.environment()))
                .credentials(credentials).build()) {
            // CertProbe uses cert-based XAdES auth (handled internally by authenticate())
            client.authenticate();
            run(client, properties.nipIdentifier());
        } catch (Exception exception) {
            LOG.error("Probe failed", exception);
            System.exit(EXIT_FAILURE);
        }
    }

    private static void run(KsefClient client, String nipIdentifier) throws Exception {
        // STEP 1: Already authenticated via client.authenticate() in main
        section("STEP 1: XAdES authentication (handled by SDK)");
        LOG.info("XAdES session active (authenticated via KsefClient.authenticate())");

        // STEP 2: Limits BEFORE
        section("STEP 2: certificates/limits BEFORE");
        CertificateLimits limitsBefore = client.certificates().getLimits();
        printLimits("BEFORE", limitsBefore);

        // STEP 3: Query active certs
        section("STEP 3: query active certificates");
        CertificateQueryResult queryResult = client.certificates().query(CertificateQueryBuilder.create());
        List<CertificateListItem> certs = queryResult.certificates();
        LOG.info("Found {} certificates total", certs.size());
        for (CertificateListItem cert : certs) {
            LOG.info("  serial={} status={} validFrom={} requestDate={} name={}",
                    cert.certificateSerialNumber(), cert.status(), cert.validFrom(), cert.requestDate(), cert.name());
        }
        List<CertificateListItem> active = certs.stream()
                .filter(cert -> STATUS_ACTIVE.equalsIgnoreCase(cert.status()))
                .toList();
        LOG.info("Active count: {}", active.size());

        if (active.isEmpty()) {
            LOG.warn("No active certs to revoke. Skipping revoke step.");
        } else {
            // STEP 4: Revoke youngest
            section("STEP 4: revoke youngest active cert");
            CertificateListItem youngest = active.stream()
                    .max(Comparator.comparing(CertProbe::certSortKey,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElseThrow();
            LOG.info("Youngest active: serial={} validFrom={} name={}",
                    youngest.certificateSerialNumber(), youngest.validFrom(), youngest.name());
            try {
                client.certificates().revoke(youngest.certificateSerialNumber());
                LOG.info("REVOKE OK serial={}", youngest.certificateSerialNumber());
            } catch (Exception exception) {
                LOG.error("REVOKE FAILED", exception);
            }

            // STEP 5: Limits AFTER revoke
            section("STEP 5: certificates/limits AFTER revoke");
            CertificateLimits limitsAfterRevoke = client.certificates().getLimits();
            printLimits("AFTER_REVOKE", limitsAfterRevoke);
        }

        // STEP 6: Enroll new
        section("STEP 6: enroll new cert");
        CertificateEnrollmentData enrollData = client.certificates().getEnrollmentData();
        LOG.info("Enrollment data: cn={}", enrollData.commonName());
        KeyPair newKeyPair = generateKeyPair();
        byte[] csrBytes = generateCsr(enrollData, newKeyPair);
        CertificateEnrollBuilder enrollBuilder = CertificateEnrollBuilder.create(
                CERT_NAME, KsefCertificateTypeRaw.AUTHENTICATION, csrBytes);

        EnrollCertificateResult enrollResult;
        try {
            enrollResult = client.certificates().enroll(enrollBuilder);
            LOG.info("ENROLL OK ref={}", enrollResult.referenceNumber());
        } catch (Exception exception) {
            LOG.error("ENROLL FAILED", exception);
            section("STEP 8: certificates/limits AFTER failed enroll");
            printLimits("AFTER_FAILED_ENROLL", client.certificates().getLimits());
            return;
        }

        // STEP 7: Poll for serial
        section("STEP 7: poll enrollment status for serial number");
        String enrolledSerial = pollEnrollmentSerial(client, enrollResult.referenceNumber());

        // STEP 8: Limits AFTER enroll
        section("STEP 8: certificates/limits AFTER enroll");
        printLimits("AFTER_ENROLL", client.certificates().getLimits());

        // STEP 9: Cleanup new cert
        if (enrolledSerial != null) {
            section("STEP 9: cleanup — revoke newly enrolled cert");
            try {
                client.certificates().revoke(enrolledSerial);
                LOG.info("CLEANUP REVOKE OK serial={}", enrolledSerial);
            } catch (Exception exception) {
                LOG.error("CLEANUP REVOKE FAILED", exception);
            }
        } else {
            LOG.warn("No serial obtained — cannot cleanup. Cert may still consume slot.");
        }

        section("PROBE COMPLETE");
    }

    private static String pollEnrollmentSerial(KsefClient client, String enrollmentRef) throws InterruptedException {
        int delay = POLL_INITIAL_DELAY_MS;
        long startTime = System.currentTimeMillis();
        long deadline = startTime + ENROLL_POLL_TIMEOUT_MS;
        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try {
                CertificateEnrollmentStatus status = client.certificates().getEnrollmentStatus(enrollmentRef);
                String code = status.status() != null ? Integer.toString(status.status().code()) : "null";
                String description = status.status() != null ? status.status().description() : "null";
                LOG.info("  poll #{} status code={} desc={} serial={}",
                        attempt, code, description, status.certificateSerialNumber());
                if (status.certificateSerialNumber() != null) {
                    LOG.info("SERIAL OBTAINED after {}ms: {}",
                            System.currentTimeMillis() - startTime,
                            status.certificateSerialNumber());
                    return status.certificateSerialNumber();
                }
            } catch (Exception exception) {
                LOG.warn("  poll #{} failed (will retry): {}", attempt, exception.getMessage());
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }
        LOG.warn("TIMEOUT after {}ms — serial never appeared", ENROLL_POLL_TIMEOUT_MS);
        return null;
    }

    private static void pollAuth(KsefClient client, String authRef) throws InterruptedException {
        int delay = POLL_INITIAL_DELAY_MS;
        long deadline = System.currentTimeMillis() + AUTH_POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            AuthenticationStatus authStatus = client.auth().getStatus(authRef);
            if (authStatus.status() != null && authStatus.status().code() == AUTH_STATUS_OK) {
                return;
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }
        throw new IllegalStateException("Auth timeout for " + authRef);
    }

    private static java.time.OffsetDateTime certSortKey(CertificateListItem cert) {
        return cert.validFrom() != null ? cert.validFrom() : cert.requestDate();
    }

    private static void printLimits(String label, CertificateLimits limits) {
        LOG.info("[{}] canRequest={}", label, limits.canRequest());
        if (limits.enrollment() != null) {
            LOG.info("[{}] enrollment: remaining={} limit={}",
                    label, limits.enrollment().remaining(), limits.enrollment().limit());
        }
        if (limits.certificate() != null) {
            LOG.info("[{}] certificate: remaining={} limit={}",
                    label, limits.certificate().remaining(), limits.certificate().limit());
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    private static byte[] generateCsr(CertificateEnrollmentData data, KeyPair keyPair) throws Exception {
        StringBuilder subjectDn = new StringBuilder();
        subjectDn.append(DN_CN_PREFIX).append(data.commonName());
        subjectDn.append(DN_COUNTRY_PREFIX).append(data.countryName());
        if (data.givenName() != null) {
            subjectDn.append(DN_GIVEN_NAME_PREFIX).append(data.givenName());
        }
        if (data.surname() != null) {
            subjectDn.append(DN_SURNAME_PREFIX).append(data.surname());
        }
        if (data.serialNumber() != null) {
            subjectDn.append(DN_SERIAL_NUMBER_PREFIX).append(data.serialNumber());
        }
        if (data.organizationName() != null) {
            subjectDn.append(DN_ORGANIZATION_PREFIX).append(data.organizationName());
        }
        if (data.organizationIdentifier() != null) {
            subjectDn.append(DN_ORGANIZATION_IDENTIFIER_PREFIX).append(data.organizationIdentifier());
        }
        X500Name subject = new X500Name(subjectDn.toString());
        JcaPKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
        PKCS10CertificationRequest certificationRequest = csrBuilder.build(signer);
        return certificationRequest.getEncoded();
    }

    private static void section(String title) {
        LOG.info(SEPARATOR);
        LOG.info(title);
        LOG.info(SEPARATOR);
    }
}
