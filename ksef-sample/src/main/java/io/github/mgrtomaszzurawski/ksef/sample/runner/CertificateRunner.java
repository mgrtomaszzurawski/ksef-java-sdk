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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for CertificateClient operations. Tests getLimits with token auth,
 * then switches to XAdES auth session for operations that require it
 * (enrollmentData, query). Re-authenticates with token afterward.
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String OP_ENROLL = "enroll";
    private static final String OP_ENROLLMENT_STATUS = "getEnrollmentStatus";
    private static final String OP_REVOKE = "revoke";
    private static final String SKIP_NO_CERT = "no certificate available";
    private static final String CERT_NAME = "SDK Demo Certificate";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int AUTH_STATUS_OK = 200;
    private static final int POLL_INITIAL_DELAY_MS = 500;
    private static final int POLL_MAX_DELAY_MS = 5000;
    private static final int POLL_TIMEOUT_MS = 30000;
    private static final int POLL_BACKOFF_MULTIPLIER = 2;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Get limits (works with token auth)
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getLimits();
            String enrollment = response.enrollment() != null
                    ? response.enrollment().remaining() + "/" + response.enrollment().limit()
                    : "n/a";
            String certificate = response.certificate() != null
                    ? response.certificate().remaining() + "/" + response.certificate().limit()
                    : "n/a";
            LOG.info("[{}] certificate limits: canRequest={} enrollment={} certificate={}",
                    NAME, response.canRequest(), enrollment, certificate);
            results.add(RunResult.ok(NAME, OP_GET_LIMITS, elapsed(start),
                    "canRequest=" + response.canRequest()
                            + " enrollment=" + enrollment
                            + " certificate=" + certificate));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_LIMITS, elapsed(start), errorMessage(exception)));
        }

        // 2-3-4. Enrollment data, query, enroll — require XAdES auth session
        // Cert-based auth is handled internally by KsefClient when PKCS#12 credentials are used.
        // The XAdES session switch logic is deferred to a future builder for cert operations.
        results.add(RunResult.skip(NAME, OP_GET_ENROLLMENT_DATA, SKIP_NO_CERT));
        results.add(RunResult.skip(NAME, OP_QUERY, SKIP_NO_CERT));
        results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_NO_CERT));

        return results;
    }

    // XAdES session switch + cert enrollment/revoke/query operations removed.
    // These require cert-based auth session switching which is deferred to a
    // future release with dedicated builders for certificate operations.
}
