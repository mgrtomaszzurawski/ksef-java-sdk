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
/**
 * Result of a single demo operation within a runner.
 *
 * @param runner the runner name (e.g. "auth", "session")
 * @param operation the operation name (e.g. "requestChallenge", "openOnline")
 * @param status outcome status
 * @param durationMs wall-clock time in milliseconds
 * @param message additional detail (error message for FAIL, reason for SKIP)
 */
public record RunResult(
        String runner,
        String operation,
        RunStatus status,
        long durationMs,
        String message
) {

    public static RunResult ok(String runner, String operation, long durationMs) {
        return new RunResult(runner, operation, RunStatus.OK, durationMs, null);
    }

    public static RunResult ok(String runner, String operation, long durationMs, String message) {
        return new RunResult(runner, operation, RunStatus.OK, durationMs, message);
    }

    public static RunResult fail(String runner, String operation, long durationMs, String message) {
        return new RunResult(runner, operation, RunStatus.FAIL, durationMs, message);
    }

    public static RunResult skip(String runner, String operation, String reason) {
        return new RunResult(runner, operation, RunStatus.SKIP, 0, reason);
    }

    @Override
    public String toString() {
        String label = String.format("[%-4s] %s.%s", status, runner, operation);
        if (message != null) {
            return label + " - " + message + " (" + durationMs + "ms)";
        }
        return label + " (" + durationMs + "ms)";
    }
}
