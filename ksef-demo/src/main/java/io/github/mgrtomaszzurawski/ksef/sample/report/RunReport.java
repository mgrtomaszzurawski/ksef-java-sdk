/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.report;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aggregates results from all demo runners and prints a summary report.
 */
public final class RunReport {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunReport.class);
    private static final String SUMMARY_FORMAT = "--- Summary: {} OK, {} FAIL, {} SKIP ---";
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;

    private final List<RunResult> results = new ArrayList<>();

    public void add(RunResult result) {
        results.add(result);
    }

    public void addAll(List<RunResult> resultList) {
        results.addAll(resultList);
    }

    public long okCount() {
        return results.stream().filter(result -> result.status() == RunStatus.OK).count();
    }

    public long failCount() {
        return results.stream().filter(result -> result.status() == RunStatus.FAIL).count();
    }

    public long skipCount() {
        return results.stream().filter(result -> result.status() == RunStatus.SKIP).count();
    }

    public boolean hasFailures() {
        return failCount() > 0;
    }

    public int exitCode() {
        return hasFailures() ? EXIT_FAILURE : EXIT_SUCCESS;
    }

    /**
     * Print all results and a summary line.
     */
    public void print() {
        for (RunResult result : results) {
            if (result.status() == RunStatus.FAIL) {
                LOGGER.error("{}", result);
            } else {
                LOGGER.info("{}", result);
            }
        }
        LOGGER.info(SUMMARY_FORMAT, okCount(), failCount(), skipCount());
    }
}
