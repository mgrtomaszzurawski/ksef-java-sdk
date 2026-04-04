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

import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for CertificateClient read operations. Checks limits, enrollment data,
 * and queries existing certificates.
 *
 * <p>Certificate enrollment (enroll+revoke) is skipped — it requires CSR generation
 * which is a separate complex operation. Read-only operations are tested.</p>
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String OP_ENROLL = "enroll";
    private static final String SKIP_ENROLL = "requires CSR generation";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Get limits
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getLimits();
            LOG.info("[{}] certificate limits: canRequest={}", NAME, response.getCanRequest());
            results.add(RunResult.ok(NAME, OP_GET_LIMITS, elapsed(start),
                    "canRequest=" + response.getCanRequest()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_LIMITS, elapsed(start), errorMessage(exception)));
        }

        // 2. Get enrollment data
        start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getEnrollmentData();
            LOG.info("[{}] enrollment data: commonName={}, country={}", NAME,
                    response.getCommonName(), response.getCountryName());
            results.add(RunResult.ok(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start), errorMessage(exception)));
        }

        // 3. Query certificates
        start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().query(new QueryCertificatesRequestRaw());
            int count = response.getCertificates() != null ? response.getCertificates().size() : 0;
            LOG.info("[{}] queried certificates: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY, elapsed(start), count + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY, elapsed(start), errorMessage(exception)));
        }

        // 4. Enroll + revoke — skipped (needs CSR)
        results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_ENROLL));

        return results;
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static String errorMessage(Exception exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
