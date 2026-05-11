/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
 * Runner for the rate-limits endpoint, exposed via the merged
 * {@code client.limits()} accessor (PR7 — RateLimitClient was folded
 * into LimitsClient since KSeF docs describe both as "limity").
 */
public final class RateLimitRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitRunner.class);
    private static final String NAME = "ratelimit";
    private static final String OP_GET = "getRateLimits";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        long start = System.currentTimeMillis();
        try {
            context.client().limits().getRateLimits();
            LOGGER.info("[{}] rate limits fetched", NAME);
            results.add(RunResult.ok(NAME, OP_GET, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET, elapsed(start), errorMessage(exception)));
        }
        return results;
    }
}
