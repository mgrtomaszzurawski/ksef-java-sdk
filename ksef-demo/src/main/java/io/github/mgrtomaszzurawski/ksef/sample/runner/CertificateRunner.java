/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for Certificates operations.
 *
 * <p>{@code getLimits} and {@code queryCertificates} work with any auth
 * method. {@code requestNewCertificate} (R1-20 workflow) requires XAdES
 * auth and consumes a monthly enrollment slot — it is skipped by
 * default to preserve the spec-enforced quota of 12 enrollments/month.
 * Set {@code -Ddemo.cert.test=true} to opt in to a single enroll →
 * revoke round-trip.
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_QUERY = "query";
    private static final String OP_REQUEST_NEW = "requestNewCertificate";
    private static final String OP_REVOKE = "revoke";
    private static final String SKIP_QUOTA =
            "skipped to preserve cert quota (max 12/month) — enable with -Ddemo.cert.test=true";
    private static final String SKIP_REVOKE_NO_SERIAL =
            "skipped — requestNewCertificate did not yield a usable certificate";

    private static final String CERT_TEST_PROPERTY = "demo.cert.test";
    private static final String FLAG_TRUE = "true";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        runGetLimits(context, results);
        runQuery(context, results);
        runEnrollRevokeCycle(context, results);
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

    private void runQuery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateQueryResult response = context.client().certificates()
                    .queryCertificates(CertificateQueryBuilder.create().build());
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

    private void runEnrollRevokeCycle(DemoContext context, List<RunResult> results) {
        if (!FLAG_TRUE.equals(System.getProperty(CERT_TEST_PROPERTY))) {
            results.add(RunResult.skip(NAME, OP_REQUEST_NEW, SKIP_QUOTA));
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_QUOTA));
            return;
        }
        long start = System.currentTimeMillis();
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyGen.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();
            RetrievedCertificate retrieved = context.client().certificates().requestNewCertificate(keyPair);
            io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber serial =
                    retrieved.certificateSerialNumber();
            LOGGER.info("[{}] requested new certificate, serial={}", NAME, serial);
            results.add(RunResult.ok(NAME, OP_REQUEST_NEW, elapsed(start), "serial=" + serial));
            runRevoke(context, serial, results);
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REQUEST_NEW, elapsed(start), errorMessage(exception)));
            results.add(RunResult.skip(NAME, OP_REVOKE, SKIP_REVOKE_NO_SERIAL));
        }
    }

    private void runRevoke(DemoContext context,
                           io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateSerialNumber serial,
                           List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().certificates().revoke(serial, CertificateRevocationReason.UNSPECIFIED);
            LOGGER.info("[{}] revoked certificate, serial={}", NAME, serial);
            results.add(RunResult.ok(NAME, OP_REVOKE, elapsed(start), "serial=" + serial));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE, elapsed(start), errorMessage(exception)));
        }
    }
}
