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

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import java.util.List;

/**
 * Interface for demo runners. Each runner exercises one SDK domain client.
 */
public interface DemoRunner {

    /**
     * Runner name used in logging and report (e.g. "auth", "session").
     */
    String name();

    /**
     * Execute operations and return results. Each operation produces one RunResult.
     * Implementations should catch per-operation exceptions and return FAIL results
     * rather than propagating, so one failure does not skip remaining operations.
     *
     * @param context shared demo context with client, mode, and inter-runner state
     * @return list of results, one per operation attempted
     */
    List<RunResult> run(DemoContext context);
}
