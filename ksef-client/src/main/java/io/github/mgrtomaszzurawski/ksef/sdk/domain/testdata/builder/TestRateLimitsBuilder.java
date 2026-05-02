/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;

import io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitValuesOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.ApiRateLimitsOverrideRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw;

/**
 * Builder for KSeF test rate limits override requests.
 * <p>
 * Required: rateLimits (at least one category must be set).
 * Each category has perSecond, perMinute, perHour values.
 * <p>
 * Usage:
 * <pre>{@code
 * SetRateLimitsRequestRaw request = TestRateLimitsBuilder.create()
 *     .onlineSession(10, 100, 1000)
 *     .invoiceSend(5, 50, 500)
 *     .build();
 * }</pre>
 */
public final class TestRateLimitsBuilder {

    private static final String ERR_NO_LIMITS_SET = "at least one rate limit category must be set";

    private ApiRateLimitValuesOverrideRaw onlineSession;
    private ApiRateLimitValuesOverrideRaw batchSession;
    private ApiRateLimitValuesOverrideRaw invoiceSend;
    private ApiRateLimitValuesOverrideRaw invoiceStatus;
    private ApiRateLimitValuesOverrideRaw sessionList;
    private ApiRateLimitValuesOverrideRaw sessionInvoiceList;
    private ApiRateLimitValuesOverrideRaw sessionMisc;
    private ApiRateLimitValuesOverrideRaw invoiceMetadata;
    private ApiRateLimitValuesOverrideRaw invoiceExport;
    private ApiRateLimitValuesOverrideRaw invoiceExportStatus;
    private ApiRateLimitValuesOverrideRaw invoiceDownload;
    private ApiRateLimitValuesOverrideRaw other;

    private TestRateLimitsBuilder() {
    }

    /**
     * Create a new rate limits builder.
     */
    public static TestRateLimitsBuilder create() {
        return new TestRateLimitsBuilder();
    }

    private static ApiRateLimitValuesOverrideRaw rateValues(int perSecond, int perMinute, int perHour) {
        ApiRateLimitValuesOverrideRaw values = new ApiRateLimitValuesOverrideRaw();
        values.setPerSecond(perSecond);
        values.setPerMinute(perMinute);
        values.setPerHour(perHour);
        return values;
    }

    public TestRateLimitsBuilder onlineSession(int perSecond, int perMinute, int perHour) {
        this.onlineSession = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder batchSession(int perSecond, int perMinute, int perHour) {
        this.batchSession = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceSend(int perSecond, int perMinute, int perHour) {
        this.invoiceSend = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceStatus(int perSecond, int perMinute, int perHour) {
        this.invoiceStatus = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionList(int perSecond, int perMinute, int perHour) {
        this.sessionList = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionInvoiceList(int perSecond, int perMinute, int perHour) {
        this.sessionInvoiceList = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder sessionMisc(int perSecond, int perMinute, int perHour) {
        this.sessionMisc = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceMetadata(int perSecond, int perMinute, int perHour) {
        this.invoiceMetadata = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceExport(int perSecond, int perMinute, int perHour) {
        this.invoiceExport = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceExportStatus(int perSecond, int perMinute, int perHour) {
        this.invoiceExportStatus = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder invoiceDownload(int perSecond, int perMinute, int perHour) {
        this.invoiceDownload = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    public TestRateLimitsBuilder other(int perSecond, int perMinute, int perHour) {
        this.other = rateValues(perSecond, perMinute, perHour);
        return this;
    }

    /**
     * Return a fresh builder pre-populated with this builder's current field values.
     */
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

    /**
     * Build the rate limits request.
     *
     * @return the request ready to pass to {@code TestDataClient.setRateLimits()}
     * @throws IllegalStateException if no rate limit category has been set
     *
     * @apiNote internal — SDK plumbing only; do not call from consumer code (see ADR-018).
     */
    public SetRateLimitsRequestRaw build() {
        boolean hasAny = onlineSession != null || batchSession != null || invoiceSend != null
                || invoiceStatus != null || sessionList != null || sessionInvoiceList != null
                || sessionMisc != null || invoiceMetadata != null || invoiceExport != null
                || invoiceExportStatus != null || invoiceDownload != null || other != null;
        if (!hasAny) {
            throw new IllegalStateException(ERR_NO_LIMITS_SET);
        }

        ApiRateLimitsOverrideRaw rateLimits = new ApiRateLimitsOverrideRaw();
        if (onlineSession != null) {
            rateLimits.setOnlineSession(onlineSession);
        }
        if (batchSession != null) {
            rateLimits.setBatchSession(batchSession);
        }
        if (invoiceSend != null) {
            rateLimits.setInvoiceSend(invoiceSend);
        }
        if (invoiceStatus != null) {
            rateLimits.setInvoiceStatus(invoiceStatus);
        }
        if (sessionList != null) {
            rateLimits.setSessionList(sessionList);
        }
        if (sessionInvoiceList != null) {
            rateLimits.setSessionInvoiceList(sessionInvoiceList);
        }
        if (sessionMisc != null) {
            rateLimits.setSessionMisc(sessionMisc);
        }
        if (invoiceMetadata != null) {
            rateLimits.setInvoiceMetadata(invoiceMetadata);
        }
        if (invoiceExport != null) {
            rateLimits.setInvoiceExport(invoiceExport);
        }
        if (invoiceExportStatus != null) {
            rateLimits.setInvoiceExportStatus(invoiceExportStatus);
        }
        if (invoiceDownload != null) {
            rateLimits.setInvoiceDownload(invoiceDownload);
        }
        if (other != null) {
            rateLimits.setOther(other);
        }

        SetRateLimitsRequestRaw request = new SetRateLimitsRequestRaw();
        request.setRateLimits(rateLimits);
        return request;
    }
}
