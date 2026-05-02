/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitValues;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;

/**
 * Builder for KSeF test rate limits override requests.
 * <p>At least one rate-limit category must be set.
 */
public final class TestRateLimitsBuilder {

    private static final String ERR_NO_LIMITS_SET = "at least one rate limit category must be set";

    private TestRateLimitValues onlineSession;
    private TestRateLimitValues batchSession;
    private TestRateLimitValues invoiceSend;
    private TestRateLimitValues invoiceStatus;
    private TestRateLimitValues sessionList;
    private TestRateLimitValues sessionInvoiceList;
    private TestRateLimitValues sessionMisc;
    private TestRateLimitValues invoiceMetadata;
    private TestRateLimitValues invoiceExport;
    private TestRateLimitValues invoiceExportStatus;
    private TestRateLimitValues invoiceDownload;
    private TestRateLimitValues other;

    private TestRateLimitsBuilder() { }

    public static TestRateLimitsBuilder create() {
        return new TestRateLimitsBuilder();
    }

    public TestRateLimitsBuilder onlineSession(int perSecond, int perMinute, int perHour) {
        this.onlineSession = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder batchSession(int perSecond, int perMinute, int perHour) {
        this.batchSession = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceSend(int perSecond, int perMinute, int perHour) {
        this.invoiceSend = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceStatus(int perSecond, int perMinute, int perHour) {
        this.invoiceStatus = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionList(int perSecond, int perMinute, int perHour) {
        this.sessionList = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionInvoiceList(int perSecond, int perMinute, int perHour) {
        this.sessionInvoiceList = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionMisc(int perSecond, int perMinute, int perHour) {
        this.sessionMisc = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceMetadata(int perSecond, int perMinute, int perHour) {
        this.invoiceMetadata = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceExport(int perSecond, int perMinute, int perHour) {
        this.invoiceExport = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceExportStatus(int perSecond, int perMinute, int perHour) {
        this.invoiceExportStatus = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceDownload(int perSecond, int perMinute, int perHour) {
        this.invoiceDownload = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder other(int perSecond, int perMinute, int perHour) {
        this.other = new TestRateLimitValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder toBuilder() {
        TestRateLimitsBuilder copy = new TestRateLimitsBuilder();
        copy.onlineSession = this.onlineSession;
        copy.batchSession = this.batchSession;
        copy.invoiceSend = this.invoiceSend;
        copy.invoiceStatus = this.invoiceStatus;
        copy.sessionList = this.sessionList;
        copy.sessionInvoiceList = this.sessionInvoiceList;
        copy.sessionMisc = this.sessionMisc;
        copy.invoiceMetadata = this.invoiceMetadata;
        copy.invoiceExport = this.invoiceExport;
        copy.invoiceExportStatus = this.invoiceExportStatus;
        copy.invoiceDownload = this.invoiceDownload;
        copy.other = this.other;
        return copy;
    }

    public TestRateLimitsRequest build() {
        boolean hasAny = onlineSession != null || batchSession != null || invoiceSend != null
                || invoiceStatus != null || sessionList != null || sessionInvoiceList != null
                || sessionMisc != null || invoiceMetadata != null || invoiceExport != null
                || invoiceExportStatus != null || invoiceDownload != null || other != null;
        if (!hasAny) {
            throw new IllegalStateException(ERR_NO_LIMITS_SET);
        }
        return new TestRateLimitsRequest(onlineSession, batchSession, invoiceSend, invoiceStatus,
                sessionList, sessionInvoiceList, sessionMisc, invoiceMetadata,
                invoiceExport, invoiceExportStatus, invoiceDownload, other);
    }
}
