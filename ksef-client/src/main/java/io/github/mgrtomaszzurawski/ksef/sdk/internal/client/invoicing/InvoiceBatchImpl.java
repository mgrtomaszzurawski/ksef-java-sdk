/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificateUsage;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceBatch;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.BatchSubmissionFlow;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session.SessionClient;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch submission implementation of {@link InvoiceBatch}.
 *
 * @since 1.0.0
 */
public final class InvoiceBatchImpl implements InvoiceBatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceBatchImpl.class);
    private static final String LOG_CALL = "→ {}";
    private static final String OP_SUBMIT_BATCH = "submitBatch";
    private static final String OP_SUBMIT_BATCH_FROM_FILES = "submitBatchFromFiles";

    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_NULL_INVOICES = "invoices must not be null";
    private static final String ERR_NULL_OPTIONS = "options must not be null";
    private static final String ERR_NULL_FILES = "files must not be null";

    private final SessionClient sessionClient;
    private final HttpClient httpClient;
    private final KsefEnvironment environment;
    private final Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver;

    public InvoiceBatchImpl(SessionClient sessionClient,
                            HttpClient httpClient,
                            KsefEnvironment environment,
                            Function<PublicKeyCertificateUsage, PublicKey> publicKeyResolver) {
        this.sessionClient = Objects.requireNonNull(sessionClient, "sessionClient must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
        this.publicKeyResolver = Objects.requireNonNull(publicKeyResolver, "publicKeyResolver must not be null");
    }

    @Override
    public BatchResult submit(FormCode formCode, List<Invoice> invoices, BatchOptions options) {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        Objects.requireNonNull(invoices, ERR_NULL_INVOICES);
        Objects.requireNonNull(options, ERR_NULL_OPTIONS);
        LOGGER.debug(LOG_CALL, OP_SUBMIT_BATCH);
        return newFlow().submit(formCode, invoices, options);
    }

    @Override
    public BatchResult submitFromFiles(FormCode formCode, List<Path> files, BatchOptions options) {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        Objects.requireNonNull(files, ERR_NULL_FILES);
        Objects.requireNonNull(options, ERR_NULL_OPTIONS);
        LOGGER.debug(LOG_CALL, OP_SUBMIT_BATCH_FROM_FILES);
        return newFlow().submitFromFiles(formCode, files, options);
    }

    private BatchSubmissionFlow newFlow() {
        return new BatchSubmissionFlow(sessionClient, httpClient, environment, publicKeyResolver);
    }
}
