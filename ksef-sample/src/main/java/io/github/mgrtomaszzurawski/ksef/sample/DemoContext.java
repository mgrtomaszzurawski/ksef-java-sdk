/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefIdentifier;

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
    private String sessionReferenceNumber;
    private String invoiceReferenceNumber;
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

    public String sessionReferenceNumber() { return sessionReferenceNumber; }
    public void setSessionReferenceNumber(String ref) { this.sessionReferenceNumber = ref; }

    public String invoiceReferenceNumber() { return invoiceReferenceNumber; }
    public void setInvoiceReferenceNumber(String ref) { this.invoiceReferenceNumber = ref; }

    public String invoiceKsefNumber() { return invoiceKsefNumber; }
    public void setInvoiceKsefNumber(String ksefNumber) { this.invoiceKsefNumber = ksefNumber; }
}
