/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

/**
 * Configurable retry policy for KSeF API calls.
 */
public record RetryPolicy(
        boolean enabled,
        int maxAttempts,
        boolean retryOn5xx,
        boolean retryOn429,
        boolean retryPost,
        long maxRetryAfterSeconds,
        BackoffStrategy backoffStrategy
) {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_MAX_RETRY_AFTER_SECONDS = 60L;

    public enum BackoffStrategy {
        FIXED,
        EXPONENTIAL
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private boolean enabled = true;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private boolean retryOn5xx = true;
        private boolean retryOn429 = true;
        private boolean retryPost = false;
        private long maxRetryAfterSeconds = DEFAULT_MAX_RETRY_AFTER_SECONDS;
        private BackoffStrategy backoffStrategy = BackoffStrategy.EXPONENTIAL;

        private Builder() {
        }

        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder maxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; return this; }
        public Builder retryOn5xx(boolean retryOn5xx) { this.retryOn5xx = retryOn5xx; return this; }
        public Builder retryOn429(boolean retryOn429) { this.retryOn429 = retryOn429; return this; }
        public Builder retryPost(boolean retryPost) { this.retryPost = retryPost; return this; }
        public Builder maxRetryAfterSeconds(long maxRetryAfterSeconds) { this.maxRetryAfterSeconds = maxRetryAfterSeconds; return this; }
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) { this.backoffStrategy = backoffStrategy; return this; }

        public RetryPolicy build() {
            return new RetryPolicy(enabled, maxAttempts, retryOn5xx, retryOn429,
                    retryPost, maxRetryAfterSeconds, backoffStrategy);
        }
    }
}
