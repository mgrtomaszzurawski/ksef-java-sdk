/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sample.util.CertificateCsrUtil;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefPkcs12Credentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(CertProbe.class);
    private static final Path CREDENTIALS_FILE = Path.of("ksef-credentials.properties");
    private static final String CERT_NAME = "Probe Cert RCA Verify";
    private static final String STATUS_ACTIVE = "Active";
    private static final int POLL_INITIAL_DELAY_MS = 1000;
    private static final int POLL_MAX_DELAY_MS = 10000;
    /** 5 minutes — enrollment can take a while in the test environment. */
    private static final int ENROLL_POLL_TIMEOUT_MS = 300000;
    private static final int POLL_BACKOFF_MULTIPLIER = 2;
    private static final int EXIT_FAILURE = 1;
    private static final String SEPARATOR = "================================================================";

    private CertProbe() { }

    public static void main(String[] args) {
        AppProperties properties = AppProperties.load(CREDENTIALS_FILE);
        if (!properties.hasCertificate()) {
            LOGGER.error("No certificate configured in {}", CREDENTIALS_FILE);
            System.exit(EXIT_FAILURE);
        }

        KsefPkcs12Credentials credentials = new KsefPkcs12Credentials(
                Path.of(properties.certFile()),
                properties.certPassword().toCharArray(),
                properties.nipIdentifier());

        try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.custom(properties.environment()))
                .credentials(credentials).build()) {
            // Drive lazy auth via any authenticated read.
            client.auth().streamSessions().findAny();
            run(client);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOGGER.error("Probe interrupted", interrupted);
            System.exit(EXIT_FAILURE);
        } catch (Exception exception) {
            LOGGER.error("Probe failed", exception);
            System.exit(EXIT_FAILURE);
        }
    }

    private static void run(KsefClient client)
            throws InterruptedException,
                   java.security.NoSuchAlgorithmException,
                   org.bouncycastle.operator.OperatorCreationException,
                   java.io.IOException {
        section("STEP 1: XAdES authentication (handled by SDK)");
        LOGGER.info("XAdES session active (authenticated via KsefClient lazy-auth flow)");

        section("STEP 2: certificates/limits BEFORE");
        printLimits("BEFORE", client.certificates().getLimits());

        queryAndRevokeYoungestActive(client);
        EnrollCertificateResult enrollResult = enrollNewCert(client);
        if (enrollResult == null) {
            return;
        }
        cleanupEnrolled(client, enrollResult);

        section("PROBE COMPLETE");
    }

    private static void queryAndRevokeYoungestActive(KsefClient client) {
        section("STEP 3: query active certificates");
        CertificateQueryResult queryResult = client.certificates().query(CertificateQueryBuilder.create().build());
        List<CertificateListItem> certs = queryResult.certificates();
        logCertificateInventory(certs);

        List<CertificateListItem> active = certs.stream()
                .filter(cert -> STATUS_ACTIVE.equalsIgnoreCase(cert.status()))
                .toList();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Active count: {}", active.size());
        }
        if (active.isEmpty()) {
            LOGGER.warn("No active certs to revoke. Skipping revoke step.");
            return;
        }
        revokeYoungest(client, active);
        section("STEP 5: certificates/limits AFTER revoke");
        printLimits("AFTER_REVOKE", client.certificates().getLimits());
    }

    private static void logCertificateInventory(List<CertificateListItem> certs) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Found {} certificates total", certs.size());
            for (CertificateListItem cert : certs) {
                LOGGER.info("  serial={} status={} validFrom={} requestDate={} name={}",
                        cert.certificateSerialNumber(), cert.status(), cert.validFrom(),
                        cert.requestDate(), cert.name());
            }
        }
    }

    private static void revokeYoungest(KsefClient client, List<CertificateListItem> active) {
        section("STEP 4: revoke youngest active cert");
        CertificateListItem youngest = active.stream()
                .max(Comparator.comparing(CertProbe::certSortKey,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow();
        String youngestSerial = youngest.certificateSerialNumber();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Youngest active: serial={} validFrom={} name={}",
                    youngestSerial, youngest.validFrom(), youngest.name());
        }
        try {
            client.certificates().revoke(youngestSerial, CertificateRevocationReason.UNSPECIFIED);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("REVOKE OK serial={}", youngestSerial);
            }
        } catch (Exception exception) {
            LOGGER.error("REVOKE FAILED", exception);
        }
    }

    private static EnrollCertificateResult enrollNewCert(KsefClient client)
            throws java.security.NoSuchAlgorithmException,
                   org.bouncycastle.operator.OperatorCreationException,
                   java.io.IOException {
        section("STEP 6: enroll new cert");
        CertificateEnrollmentData enrollData = client.certificates().getEnrollmentData();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Enrollment data: cn={}", enrollData.commonName());
        }
        CertificateCsrUtil.CsrResult csr = CertificateCsrUtil.generate(enrollData);
        CertificateEnrollBuilder enrollBuilder = CertificateEnrollBuilder.create(
                CERT_NAME, KsefCertificateType.AUTHENTICATION, csr.csrDer());

        try {
            EnrollCertificateResult enrollResult = client.certificates().enroll(enrollBuilder.build());
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("ENROLL OK ref={}", enrollResult.referenceNumber());
            }
            return enrollResult;
        } catch (Exception exception) {
            LOGGER.error("ENROLL FAILED", exception);
            section("STEP 8: certificates/limits AFTER failed enroll");
            printLimits("AFTER_FAILED_ENROLL", client.certificates().getLimits());
            return null;
        }
    }

    private static void cleanupEnrolled(KsefClient client, EnrollCertificateResult enrollResult)
            throws InterruptedException {
        section("STEP 7: poll enrollment status for serial number");
        String enrolledSerial = pollEnrollmentSerial(client, enrollResult.referenceNumber());

        section("STEP 8: certificates/limits AFTER enroll");
        printLimits("AFTER_ENROLL", client.certificates().getLimits());

        if (enrolledSerial == null) {
            LOGGER.warn("No serial obtained — cannot cleanup. Cert may still consume slot.");
            return;
        }
        section("STEP 9: cleanup — revoke newly enrolled cert");
        try {
            client.certificates().revoke(enrolledSerial, CertificateRevocationReason.UNSPECIFIED);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("CLEANUP REVOKE OK serial={}", enrolledSerial);
            }
        } catch (Exception exception) {
            LOGGER.error("CLEANUP REVOKE FAILED", exception);
        }
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
                String serial = status.certificateSerialNumber();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("  poll #{} status code={} desc={} serial={}", attempt, code, description, serial);
                }
                if (serial != null) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("SERIAL OBTAINED after {}ms: {}", System.currentTimeMillis() - startTime, serial);
                    }
                    return serial;
                }
            } catch (Exception exception) {
                LOGGER.warn("  poll #{} failed (will retry): {}", attempt, exception.getMessage());
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }
        LOGGER.warn("TIMEOUT after {}ms — serial never appeared", ENROLL_POLL_TIMEOUT_MS);
        return null;
    }

    private static java.time.OffsetDateTime certSortKey(CertificateListItem cert) {
        return cert.validFrom() != null ? cert.validFrom() : cert.requestDate();
    }

    private static void printLimits(String label, CertificateLimits limits) {
        LOGGER.info("[{}] canRequest={}", label, limits.canRequest());
        if (limits.enrollment() != null) {
            LOGGER.info("[{}] enrollment: remaining={} limit={}",
                    label, limits.enrollment().remaining(), limits.enrollment().limit());
        }
        if (limits.certificate() != null) {
            LOGGER.info("[{}] certificate: remaining={} limit={}",
                    label, limits.certificate().remaining(), limits.certificate().limit());
        }
    }

    private static void section(String title) {
        LOGGER.info(SEPARATOR);
        LOGGER.info(title);
        LOGGER.info(SEPARATOR);
    }
}
