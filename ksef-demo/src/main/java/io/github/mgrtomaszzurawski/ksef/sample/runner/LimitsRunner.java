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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for LimitsClient operations.
 */
public final class LimitsRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LimitsRunner.class);
    private static final String NAME = "limits";
    private static final String OP_CONTEXT = "getContextLimits";
    private static final String OP_SUBJECT = "getSubjectLimits";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        long start = System.currentTimeMillis();
        try {
            context.client().limits().getContextLimits();
            LOG.info("[{}] context limits fetched", NAME);
            results.add(RunResult.ok(NAME, OP_CONTEXT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CONTEXT, elapsed(start), errorMessage(exception)));
        }

        start = System.currentTimeMillis();
        try {
            context.client().limits().getSubjectLimits();
            LOG.info("[{}] subject limits fetched", NAME);
            results.add(RunResult.ok(NAME, OP_SUBJECT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SUBJECT, elapsed(start), errorMessage(exception)));
        }

        return results;
    }


}
