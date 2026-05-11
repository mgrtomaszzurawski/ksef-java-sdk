/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
 * Runner for PeppolProviders operations.
 *
 * <p>Exercises both the no-arg default-page query and the paginated query overload.
 * Requires authentication (AUTH_SAFE+ modes).</p>
 */
public final class PeppolRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolRunner.class);
    private static final String NAME = "peppol";
    private static final String OP_QUERY_DEFAULT = "queryDefault";
    private static final String OP_QUERY_PAGED = "queryPaged";
    private static final int DEFAULT_PAGE_OFFSET = 0;
    /** First-page size for the default-paging probe — small to keep wire trace tight. */
    private static final int DEFAULT_PAGE_SIZE = 10;
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
            PeppolProvidersResult response = context.client().peppol()
                    .query(DEFAULT_PAGE_OFFSET, DEFAULT_PAGE_SIZE);
            int count = response.providers() != null ? response.providers().size() : 0;
            LOGGER.info("[{}] default query: {} providers, hasMore={}", NAME, count, response.hasMore());
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
            LOGGER.info("[{}] paged query offset={} size={}: {} providers, hasMore={}",
                    NAME, PAGED_OFFSET, PAGED_SIZE, count, response.hasMore());
            results.add(RunResult.ok(NAME, OP_QUERY_PAGED, elapsed(start),
                    count + " providers"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PAGED, elapsed(start),
                    errorMessage(exception)));
        }
    }
}
