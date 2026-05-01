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
import io.github.mgrtomaszzurawski.ksef.sdk.internal.security.SecurityClient;

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for SecurityClient operations. Fetches KSeF public key certificates
 * and verifies they are available. No auth required for this endpoint.
 *
 * <p>Note: the SDK caches public keys internally during authenticate() and openSession().
 * This runner exercises the security endpoint directly as a smoke test.</p>
 */
public final class SecurityRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityRunner.class);
    private static final String NAME = "security";
    private static final String OP_GET_CERTS = "getPublicKeyCertificates";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        long start = System.currentTimeMillis();
        try {
            List<PublicKeyCertificate> certs = context.client().security()
                    .getPublicKeyCertificates();
            LOG.info("[{}] fetched {} certificates", NAME, certs.size());

            for (PublicKeyCertificate cert : certs) {
                LOG.info("[{}] cert usage={}, valid={} to {}", NAME,
                        cert.usage(), cert.validFrom(), cert.validTo());
            }

            results.add(RunResult.ok(NAME, OP_GET_CERTS, elapsed(start),
                    certs.size() + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_CERTS, elapsed(start),
                    errorMessage(exception)));
        }
        return results;
    }
}
