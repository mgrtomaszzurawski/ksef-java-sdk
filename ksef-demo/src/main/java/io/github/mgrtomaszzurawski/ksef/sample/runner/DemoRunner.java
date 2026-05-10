/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
