/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * SDK request for {@code TestDataClient.setSessionLimits(...)}.
 */
public record TestSessionLimitsRequest(TestSessionLimits onlineSession, TestSessionLimits batchSession) {

    public TestSessionLimitsRequest {
        Objects.requireNonNull(onlineSession, "onlineSession");
        Objects.requireNonNull(batchSession, "batchSession");
    }
}
