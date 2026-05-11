/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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

    public static RunResult skip(String runner, String operation, long durationMs, String reason) {
        return new RunResult(runner, operation, RunStatus.SKIP, durationMs, reason);
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
