/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    private static final int CUSTOM_MAX_ATTEMPTS = 5;
    private static final long CUSTOM_MAX_RETRY_AFTER = 120L;

    @Test
    void builder_defaults_areReasonable() {
        // when
        RetryPolicy policy = RetryPolicy.builder().build();

        // then
        assertTrue(policy.enabled());
        assertEquals(3, policy.maxAttempts());
        assertTrue(policy.retryOn5xx());
        assertTrue(policy.retryOn429());
        assertEquals(RetryPolicy.BackoffStrategy.EXPONENTIAL, policy.backoffStrategy());
    }

    @Test
    void builder_customValues_arePreserved() {
        // when
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(CUSTOM_MAX_ATTEMPTS)
                .maxRetryAfterSeconds(CUSTOM_MAX_RETRY_AFTER)
                .backoffStrategy(RetryPolicy.BackoffStrategy.FIXED)
                .build();

        // then
        assertEquals(CUSTOM_MAX_ATTEMPTS, policy.maxAttempts());
        assertEquals(CUSTOM_MAX_RETRY_AFTER, policy.maxRetryAfterSeconds());
        assertEquals(RetryPolicy.BackoffStrategy.FIXED, policy.backoffStrategy());
    }

    @Test
    void build_whenMaxAttemptsZero_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.builder().maxAttempts(0).build());
    }

    @Test
    void build_whenMaxAttemptsNegative_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.builder().maxAttempts(-1).build());
    }

    @Test
    void build_whenMaxRetryAfterNegative_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.builder().maxRetryAfterSeconds(-1).build());
    }

    @Test
    void build_whenBackoffStrategyNull_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> RetryPolicy.builder().backoffStrategy(null).build());
    }
}
