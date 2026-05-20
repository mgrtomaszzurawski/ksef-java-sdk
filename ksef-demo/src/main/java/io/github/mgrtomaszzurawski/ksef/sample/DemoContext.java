/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefIdentifier;

/**
 * Shared mutable state passed to all demo runners.
 * Runners write inter-runner dependencies here (e.g. SessionRunner stores
 * the invoice reference for InvoiceRunner to query).
 */
public final class DemoContext {

    private final KsefClient client;
    private final DemoMode mode;
    private final DemoState state;
    private final String ksefToken;
    private final String nipIdentifier;
    private final KsefIdentifier.Type identifierType;
    private final String environment;
    private String invoiceKsefNumber;

    public DemoContext(KsefClient client, DemoMode mode, DemoState state,
                       String ksefToken, String nipIdentifier,
                       KsefIdentifier.Type identifierType, String environment) {
        this.client = client;
        this.mode = mode;
        this.state = state;
        this.ksefToken = ksefToken;
        this.nipIdentifier = nipIdentifier;
        this.identifierType = identifierType;
        this.environment = environment;
    }

    public KsefClient client() { return client; }
    public DemoMode mode() { return mode; }
    public DemoState state() { return state; }
    public String ksefToken() { return ksefToken; }
    public String nipIdentifier() { return nipIdentifier; }
    public KsefIdentifier.Type identifierType() { return identifierType; }
    public String environment() { return environment; }

    public String invoiceKsefNumber() { return invoiceKsefNumber; }
    public void setInvoiceKsefNumber(String ksefNumber) { this.invoiceKsefNumber = ksefNumber; }
}
