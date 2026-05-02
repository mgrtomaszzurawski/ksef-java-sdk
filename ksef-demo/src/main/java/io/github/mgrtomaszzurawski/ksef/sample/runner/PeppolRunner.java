/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model.PeppolProvidersResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for PeppolClient operations.
 *
 * <p>Exercises both the no-arg default-page query and the paginated query overload.
 * Requires authentication (AUTH_SAFE+ modes).</p>
 */
public final class PeppolRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PeppolRunner.class);
    private static final String NAME = "peppol";
    private static final String OP_QUERY_DEFAULT = "queryDefault";
    private static final String OP_QUERY_PAGED = "queryPaged";
    private static final int PAGED_OFFSET = 0;
    private static final int PAGED_SIZE = 50;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        runQueryDefault(context, results);
        runQueryPaged(context, results);

        return results;
    }

    private void runQueryDefault(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PeppolProvidersResult response = context.client().peppol().query();
            int count = response.providers() != null ? response.providers().size() : 0;
            LOG.info("[{}] default query: {} providers, hasMore={}", NAME, count, response.hasMore());
            results.add(RunResult.ok(NAME, OP_QUERY_DEFAULT, elapsed(start),
                    count + " providers"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_DEFAULT, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runQueryPaged(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PeppolProvidersResult response = context.client().peppol()
                    .query(PAGED_OFFSET, PAGED_SIZE);
            int count = response.providers() != null ? response.providers().size() : 0;
            LOG.info("[{}] paged query offset={} size={}: {} providers, hasMore={}",
                    NAME, PAGED_OFFSET, PAGED_SIZE, count, response.hasMore());
            results.add(RunResult.ok(NAME, OP_QUERY_PAGED, elapsed(start),
                    count + " providers"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PAGED, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
