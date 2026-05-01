/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.BatchSessionContextLimitsOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.OnlineSessionContextLimitsOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw;

/**
 * Builder for KSeF test session limits override requests.
 * <p>
 * Required: onlineSession and batchSession limits.
 * <p>
 * Usage:
 * <pre>{@code
 * SetSessionLimitsRequestRaw request = TestSessionLimitsBuilder.create()
 *     .onlineSession(100, 200, 500)
 *     .batchSession(100, 200, 1000)
 *     .build();
 * }</pre>
 */
public final class TestSessionLimitsBuilder {

    private static final String ERR_ONLINE_REQUIRED = "onlineSession limits are required";
    private static final String ERR_BATCH_REQUIRED = "batchSession limits are required";

    private OnlineSessionContextLimitsOverrideRaw onlineSession;
    private BatchSessionContextLimitsOverrideRaw batchSession;

    private TestSessionLimitsBuilder() {
    }

    /**
     * Create a new session limits builder.
     */
    public static TestSessionLimitsBuilder create() {
        return new TestSessionLimitsBuilder();
    }

    /**
     * Set online session limits.
     *
     * @param maxInvoiceSizeMb max single invoice size in MB
     * @param maxInvoiceWithAttachmentSizeMb max invoice with attachment size in MB
     * @param maxInvoices max number of invoices per session
     */
    public TestSessionLimitsBuilder onlineSession(int maxInvoiceSizeMb, int maxInvoiceWithAttachmentSizeMb,
                                                   int maxInvoices) {
        OnlineSessionContextLimitsOverrideRaw limits = new OnlineSessionContextLimitsOverrideRaw();
        limits.setMaxInvoiceSizeInMB(maxInvoiceSizeMb);
        limits.setMaxInvoiceWithAttachmentSizeInMB(maxInvoiceWithAttachmentSizeMb);
        limits.setMaxInvoices(maxInvoices);
        this.onlineSession = limits;
        return this;
    }

    /**
     * Set batch session limits.
     *
     * @param maxInvoiceSizeMb max single invoice size in MB
     * @param maxInvoiceWithAttachmentSizeMb max invoice with attachment size in MB
     * @param maxInvoices max number of invoices per session
     */
    public TestSessionLimitsBuilder batchSession(int maxInvoiceSizeMb, int maxInvoiceWithAttachmentSizeMb,
                                                  int maxInvoices) {
        BatchSessionContextLimitsOverrideRaw limits = new BatchSessionContextLimitsOverrideRaw();
        limits.setMaxInvoiceSizeInMB(maxInvoiceSizeMb);
        limits.setMaxInvoiceWithAttachmentSizeInMB(maxInvoiceWithAttachmentSizeMb);
        limits.setMaxInvoices(maxInvoices);
        this.batchSession = limits;
        return this;
    }

    /**
     * Build the session limits request.
     *
     * @return the request ready to pass to {@code TestDataClient.setSessionLimits()}
     * @throws IllegalStateException if validation fails
     */
    public SetSessionLimitsRequestRaw build() {
        if (onlineSession == null) {
            throw new IllegalStateException(ERR_ONLINE_REQUIRED);
        }
        if (batchSession == null) {
            throw new IllegalStateException(ERR_BATCH_REQUIRED);
        }
        SetSessionLimitsRequestRaw request = new SetSessionLimitsRequestRaw();
        request.setOnlineSession(onlineSession);
        request.setBatchSession(batchSession);
        return request;
    }
}
