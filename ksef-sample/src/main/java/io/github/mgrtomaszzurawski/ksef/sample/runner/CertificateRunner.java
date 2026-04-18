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
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.model.CertificateQueryResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for CertificateClient operations.
 *
 * <p>getLimits works with any auth method (token or XAdES).
 * getEnrollmentData and query require XAdES auth — they run when the SDK was
 * authenticated with PKCS#12/certificate credentials. With token credentials
 * these operations are skipped (server returns 403).</p>
 *
 * <p>Enrollment and revoke are skipped to preserve cert quota (max 12/month).</p>
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String SKIP_QUOTA = "skipped to preserve cert quota (max 12/month)";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Get limits (works with any auth method)
        runGetLimits(context, results);

        // 2. Get enrollment data (requires XAdES auth, may fail with token auth)
        runGetEnrollmentData(context, results);

        // 3. Query certificates (requires XAdES auth, may fail with token auth)
        runQuery(context, results);

        // 4-5. Enroll + revoke — skipped to preserve monthly quota
        results.add(RunResult.skip(NAME, "enroll", SKIP_QUOTA));
        results.add(RunResult.skip(NAME, "revoke", SKIP_QUOTA));

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
            LOG.info("[{}] limits: canRequest={} enrollment={} certificate={}",
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

    private void runGetEnrollmentData(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateEnrollmentData response = context.client().certificates()
                    .getEnrollmentData();
            LOG.info("[{}] enrollment data: cn={}", NAME, response.commonName());
            results.add(RunResult.ok(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start),
                    "cn=" + response.commonName()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runQuery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            CertificateQueryResult response = context.client().certificates()
                    .query(CertificateQueryBuilder.create());
            List<CertificateListItem> certs = response.certificates();
            int count = certs != null ? certs.size() : 0;
            LOG.info("[{}] queried certificates: {} found", NAME, count);
            if (certs != null) {
                for (CertificateListItem cert : certs) {
                    LOG.info("[{}]   serial={} status={} name={}",
                            NAME, cert.certificateSerialNumber(), cert.status(), cert.name());
                }
            }
            results.add(RunResult.ok(NAME, OP_QUERY, elapsed(start),
                    count + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
