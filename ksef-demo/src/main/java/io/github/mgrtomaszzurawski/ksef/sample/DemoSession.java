/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sample.report.RunReport;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.runner.DemoRunner;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates demo runner execution. Runs each runner in order, catches unexpected
 * exceptions, and aggregates results into a report.
 */
public final class DemoSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoSession.class);
    private static final String LOG_HEADER = "=== {} ===";
    private static final String LOG_DONE = "[{}] done in {} ms ({} results)";
    private static final String LOG_FAIL = "[{}] UNEXPECTED FAIL in {} ms: {}";

    private final DemoContext context;

    public DemoSession(DemoContext context) {
        this.context = context;
    }

    /**
     * Execute all runners in order and return an aggregated report.
     */
    public RunReport execute(List<DemoRunner> runners) {
        RunReport report = new RunReport();
        for (DemoRunner runner : runners) {
            String runnerName = runner.name();
            String header = runnerName.toUpperCase(Locale.ROOT);
            LOGGER.info(LOG_HEADER, header);
            long start = System.currentTimeMillis();
            try {
                List<RunResult> results = runner.run(context);
                long elapsed = System.currentTimeMillis() - start;
                LOGGER.info(LOG_DONE, runnerName, elapsed, results.size());
                report.addAll(results);
            } catch (Exception exception) {
                long elapsed = System.currentTimeMillis() - start;
                String detail = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                LOGGER.error(LOG_FAIL, runnerName, elapsed, detail);
                report.add(RunResult.fail(runnerName, "UNEXPECTED", elapsed, detail));
            }
        }
        return report;
    }
}
