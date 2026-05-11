/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.CertificateCsrUtil;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for Certificates operations.
 *
 * <p>getLimits works with any auth method (token or XAdES).
 * getEnrollmentData and query require XAdES auth — they run when the SDK was
 * authenticated with PKCS#12/certificate credentials. With token credentials
 * these operations are skipped (server returns 403).</p>
 *
 * <p>Enrollment and revoke default to SKIP to preserve the monthly cert quota
 * (max 12/month). Set <code>-Ddemo.cert.test=true</code> to opt in to a single
 * enroll → poll-for-serial → revoke round-trip — useful as a smoke test on a
 * fresh quota window. The newly enrolled cert is revoked immediately after the
 * serial appears, so the only quota cost is one enrollment slot.</p>
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String OP_ENROLL = "enroll";
    private static final String OP_REVOKE = "revoke";
    private static final String SKIP_QUOTA =
            "skipped to preserve cert quota (max 12/month) — enable with -Ddemo.cert.test=true";
    private static final String SKIP_NO_ENROLLMENT_DATA =
            "skipped — getEnrollmentData failed earlier (likely token auth, requires XAdES)";
    private static final String SKIP_REVOKE_NO_SERIAL =
            "skipped — enroll did not yield a serial number, nothing to revoke";

    private static final String CERT_TEST_PROPERTY = "demo.cert.test";
    private static final String FLAG_TRUE = "true";
    private static final String CERT_NAME = "SDK Demo Cert auto-revoked";
    private static final long ENROLL_POLL_INITIAL_DELAY_MS = 1000L;
    private static final long ENROLL_POLL_MAX_DELAY_MS = 10000L;
    private static final int ENROLL_POLL_BACKOFF_MULTIPLIER = 2;
    private static final long ENROLL_POLL_TIMEOUT_MS = 300_000L;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        runGetLimits(context, results);
        CertificateEnrollmentData enrollmentData = runGetEnrollmentData(context, results);
        runQuery(context, results);
        runEnrollRevokeCycle(context, enrollmentData, results);

        return results;
    }

    private void runGetLimits(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getLimits();
            String enrollment = response.enrollment() != null
                    ? response.enrollment().remaining() + "/" + response.enrollment().limit()
                    : "n/a";
            String certificate = response.certificate() != null
                    ? response.certificate().remaining() + "/" + response.certificate().limit()
                    : "n/a";
            LOGGER.info("[{}] limits: canRequest={} enrollment={} certificate={}",
                    NAME, response.canRequest(), enrollment, certificate);
            results.add(RunResult.ok(NAME, OP_GET_LIMITS, elapsed(start),
                    "canRequest=" + response.canRequest()
                            + " enrollment=" + enrollment
                            + " certificate=" + certificate));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private CertificateEnrollmentData runGetEnrollmentData(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateEnrollmentData response = context.client().certificates()
                    .getEnrollmentData();
            String commonName = response.commonName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] enrollment data: cn={}", NAME, commonName);
            }
            results.add(RunResult.ok(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start), "cn=" + commonName));
            return response;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start),
                    errorMessage(exception)));
            return null;
        }
    }

    private void runQuery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateQueryResult response = context.client().certificates()
                    .query(CertificateQueryBuilder.create().build());
            List<CertificateListItem> certs = response.certificates();
            int count = certs != null ? certs.size() : 0;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("[{}] queried certificates: {} found", NAME, count);
                if (certs != null) {
                    for (CertificateListItem cert : certs) {
                        LOGGER.info("[{}]   serial={} status={} name={}",
                                NAME, cert.certificateSerialNumber(), cert.status(), cert.name());
                    }
                }
            }
            results.add(RunResult.ok(NAME, OP_QUERY, elapsed(start),
                    count + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runEnrollRevokeCycle(DemoContext context,
                                      CertificateEnrollmentData enrollmentData,
                                      List<RunResult> results) {
        if (!FLAG_TRUE.equals(System.getProperty(CERT_TEST_PROPERTY))) {
            results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_QUOTA));
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_QUOTA));
            return;
        }
        if (enrollmentData == null) {
            results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_NO_ENROLLMENT_DATA));
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_NO_ENROLLMENT_DATA));
            return;
        }

        String enrollmentRef = runEnroll(context, enrollmentData, results);
        if (enrollmentRef == null) {
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_REVOKE_NO_SERIAL));
            return;
        }

        String serialNumber = pollEnrollmentSerial(context, enrollmentRef);
        if (serialNumber == null) {
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_REVOKE_NO_SERIAL));
            return;
        }

        runRevoke(context, serialNumber, results);
    }

    private String runEnroll(DemoContext context,
                             CertificateEnrollmentData enrollmentData,
                             List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateCsrUtil.CsrResult csr = CertificateCsrUtil.generate(enrollmentData);
            CertificateEnrollBuilder builder = CertificateEnrollBuilder.create(
                    CERT_NAME, KsefCertificateType.AUTHENTICATION, csr.csrDer());
            EnrollCertificateResult response = context.client().certificates().enroll(builder.build());
            String referenceNumber = response.referenceNumber();
            LOGGER.info("[{}] enrolled certificate, ref={}", NAME, referenceNumber);
            results.add(RunResult.ok(NAME, OP_ENROLL, elapsed(start), "ref=" + referenceNumber));
            return referenceNumber;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_ENROLL, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String pollEnrollmentSerial(DemoContext context, String enrollmentRef) {
        long delay = ENROLL_POLL_INITIAL_DELAY_MS;
        long deadline = System.currentTimeMillis() + ENROLL_POLL_TIMEOUT_MS;
        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try {
                CertificateEnrollmentStatus status = context.client().certificates()
                        .getEnrollmentStatus(enrollmentRef);
                String code = status.status() != null ? Integer.toString(status.status().code()) : "null";
                String serial = status.certificateSerialNumber();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("[{}] enrollment poll #{} code={} serial={}", NAME, attempt, code, serial);
                }
                if (serial != null) {
                    return serial;
                }
            } catch (Exception exception) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("[{}] enrollment poll #{} failed: {}", NAME, attempt, exception.getMessage());
                }
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return null;
            }
            delay = Math.min(delay * ENROLL_POLL_BACKOFF_MULTIPLIER, ENROLL_POLL_MAX_DELAY_MS);
        }
        LOGGER.warn("[{}] enrollment poll timed out after {}ms", NAME, ENROLL_POLL_TIMEOUT_MS);
        return null;
    }

    private void runRevoke(DemoContext context, String serialNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().certificates().revoke(serialNumber, CertificateRevocationReason.UNSPECIFIED);
            LOGGER.info("[{}] revoked certificate serial={}", NAME, serialNumber);
            results.add(RunResult.ok(NAME, OP_REVOKE, elapsed(start), "serial=" + serialNumber));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE, elapsed(start), errorMessage(exception)));
        }
    }
}
