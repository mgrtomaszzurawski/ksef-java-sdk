/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

import java.util.Objects;

/**
 * SDK request for {@code TestDataAdmin.setSessionLimits(...)}.
 *
 * @since 0.1.0
 */
public record TestSessionLimitsRequest(TestSessionLimits onlineSession, TestSessionLimits batchSession) {

    public TestSessionLimitsRequest {
        Objects.requireNonNull(onlineSession, "onlineSession");
        Objects.requireNonNull(batchSession, "batchSession");
    }
}
