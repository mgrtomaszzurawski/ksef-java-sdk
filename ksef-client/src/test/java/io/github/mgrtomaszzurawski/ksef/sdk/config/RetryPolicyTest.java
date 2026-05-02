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
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final int NEGATIVE_MAX_ATTEMPTS = -1;
    private static final long NEGATIVE_MAX_RETRY_AFTER = -1L;

    @Test
    void builder_defaults_areReasonable() {
        // when
        RetryPolicy policy = RetryPolicy.builder().build();

        // then
        assertTrue(policy.enabled());
        assertEquals(DEFAULT_MAX_ATTEMPTS, policy.maxAttempts());
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
        RetryPolicy.Builder builder = RetryPolicy.builder().maxAttempts(0);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void build_whenMaxAttemptsNegative_throwsIllegalArgument() {
        RetryPolicy.Builder builder = RetryPolicy.builder().maxAttempts(NEGATIVE_MAX_ATTEMPTS);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void build_whenMaxRetryAfterNegative_throwsIllegalArgument() {
        RetryPolicy.Builder builder = RetryPolicy.builder().maxRetryAfterSeconds(NEGATIVE_MAX_RETRY_AFTER);
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void build_whenBackoffStrategyNull_throwsIllegalArgument() {
        RetryPolicy.Builder builder = RetryPolicy.builder().backoffStrategy(null);
        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
