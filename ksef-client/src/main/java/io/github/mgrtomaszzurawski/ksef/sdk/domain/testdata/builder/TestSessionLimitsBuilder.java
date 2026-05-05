/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;

/**
 * Builder for KSeF test session limits override requests.
 * <p>Required: onlineSession and batchSession limits.
 *
 * @since 1.0.0
 */
public final class TestSessionLimitsBuilder {

    private static final String ERR_ONLINE_REQUIRED = "onlineSession limits are required";
    private static final String ERR_BATCH_REQUIRED = "batchSession limits are required";

    private TestSessionLimits onlineSession;
    private TestSessionLimits batchSession;

    private TestSessionLimitsBuilder() { }

    public static TestSessionLimitsBuilder create() {
        return new TestSessionLimitsBuilder();
    }

    public TestSessionLimitsBuilder onlineSession(int maxInvoiceSizeMb, int maxInvoiceWithAttachmentSizeMb, int maxInvoices) {
        this.onlineSession = new TestSessionLimits(maxInvoiceSizeMb, maxInvoiceWithAttachmentSizeMb, maxInvoices);
        return this;
    }

    public TestSessionLimitsBuilder batchSession(int maxInvoiceSizeMb, int maxInvoiceWithAttachmentSizeMb, int maxInvoices) {
        this.batchSession = new TestSessionLimits(maxInvoiceSizeMb, maxInvoiceWithAttachmentSizeMb, maxInvoices);
        return this;
    }

    public TestSessionLimitsBuilder toBuilder() {
        TestSessionLimitsBuilder copy = new TestSessionLimitsBuilder();
        copy.onlineSession = this.onlineSession;
        copy.batchSession = this.batchSession;
        return copy;
    }

    public TestSessionLimitsRequest build() {
        if (onlineSession == null) {
            throw new IllegalStateException(ERR_ONLINE_REQUIRED);
        }
        if (batchSession == null) {
            throw new IllegalStateException(ERR_BATCH_REQUIRED);
        }
        return new TestSessionLimitsRequest(onlineSession, batchSession);
    }
}
