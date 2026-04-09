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
package io.github.mgrtomaszzurawski.ksef.sample.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates results from all demo runners and prints a summary report.
 */
public final class RunReport {

    private static final Logger LOG = LoggerFactory.getLogger(RunReport.class);
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
        return results.stream().filter(r -> r.status() == RunStatus.OK).count();
    }

    public long failCount() {
        return results.stream().filter(r -> r.status() == RunStatus.FAIL).count();
    }

    public long skipCount() {
        return results.stream().filter(r -> r.status() == RunStatus.SKIP).count();
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
                LOG.error("{}", result);
            } else {
                LOG.info("{}", result);
            }
        }
        LOG.info(SUMMARY_FORMAT, okCount(), failCount(), skipCount());
    }
}
