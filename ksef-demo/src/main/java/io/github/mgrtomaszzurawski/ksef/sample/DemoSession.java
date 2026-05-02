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

    private static final Logger LOG = LoggerFactory.getLogger(DemoSession.class);
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
            String header = runner.name().toUpperCase(Locale.ROOT);
            LOG.info(LOG_HEADER, header);
            long start = System.currentTimeMillis();
            try {
                List<RunResult> results = runner.run(context);
                long elapsed = System.currentTimeMillis() - start;
                LOG.info(LOG_DONE, runner.name(), elapsed, results.size());
                report.addAll(results);
            } catch (Exception exception) {
                long elapsed = System.currentTimeMillis() - start;
                String detail = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                LOG.error(LOG_FAIL, runner.name(), elapsed, detail);
                report.add(RunResult.fail(runner.name(), "UNEXPECTED", elapsed, detail));
            }
        }
        return report;
    }
}
