/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

module io.github.mgrtomaszzurawski.ksef {

    // SDK public API — entry point
    exports io.github.mgrtomaszzurawski.ksef.sdk;

    // Configuration: environments, identifiers, retry policy
    exports io.github.mgrtomaszzurawski.ksef.sdk.config;

    // Common shared types (StatusInfo, TokenInfo, KsefNumber, public-key models)
    exports io.github.mgrtomaszzurawski.ksef.sdk.common;

    // Auth-session management accessor (KsefClient.auth())
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.auth;

    // Authentication public DTOs (AuthSession returned by KsefClient.auth().streamSessions())
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model;

    // Public crypto facade (REQ-CRYPTO-001..004)
    exports io.github.mgrtomaszzurawski.ksef.sdk.crypto;

    // Invoicing functionality (online + batch sessions, invoice ops, QR, sync orchestrator)
    // PR11 (2026-05-09): batch submission moved to a single sync facade
    // Invoices.submitBatch(...) — public batch metadata types removed.
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync;

    // Permissions functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

    // Tokens functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model;

    // Certificates functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model;

    // Peppol functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.peppol.model;

    // Limits and rate-limit queries
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.limits;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model;

    // Test data functionality (api-test environment helpers)
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model;

    // Exception hierarchy
    exports io.github.mgrtomaszzurawski.ksef.sdk.exception;

    // Internal mechanisms (sdk.internal.*) are NOT exported.

    // PR21 (ADR-030): typed Invoice / Document classes expose flat
    // primitive accessors, but keep one JAXB escape-hatch each — Fa(2|3)
    // .faktura(), PefInvoice.invoice(), PefKorInvoice.creditNote(). Those
    // return types live at the four root xml.* packages, which therefore
    // remain on the public API surface. UBL sub-packages (xml.pef.cac,
    // xml.pef.cbc, etc.) are NO longer consumer-visible — they were only
    // needed for the old direct accessors and are dropped from exports.
    exports io.github.mgrtomaszzurawski.ksef.xml.fa2;
    exports io.github.mgrtomaszzurawski.ksef.xml.fa3;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor;

    // Required modules
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.openapitools.jackson.nullable;
    requires jakarta.annotation;
    requires jakarta.xml.bind;
    requires org.slf4j;

    // JSpecify null-safety annotations (ADR-017)
    requires static org.jspecify;

    // Crypto dependencies (BouncyCastle)
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;

    // QR code generation (ZXing - automatic modules)
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires java.desktop;

    // XAdES signing (EU DSS - automatic modules with underscores)
    requires jpms_dss_xades;
    requires jpms_dss_enumerations;
    requires jpms_dss_model;
    requires jpms_dss_token;
    requires jpms_dss_spi;
    requires jpms_dss_document;

    // Open generated model packages to Jackson for reflection-based deserialization
    opens io.github.mgrtomaszzurawski.ksef.client.model to com.fasterxml.jackson.databind;

    // Open JAXB-generated packages to jakarta.xml.bind for reflective
    // marshalling. Per ADR-030 / PR21, only the 6 root packages are
    // EXPORTED (consumer-visible). UBL sub-packages (xml.pef.cac /
    // xml.pef.cbc / etc.) are NOT exported but ARE opened — JAXB context
    // construction needs reflection access to their ObjectFactory classes
    // to resolve the qualified UBL element names that InvoiceType /
    // CreditNoteType reference. opens-to-module is reflection-only and
    // does not contribute to the public API surface.
    opens io.github.mgrtomaszzurawski.ksef.xml.fa2 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.fa3 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cacpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbcpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ccts to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ext to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.udt to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades132 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades141 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xmldsig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cacpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbcpl to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ccts to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ext to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcac to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcbc to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.udt to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades132 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades141 to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xmldsig to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.upo to jakarta.xml.bind;
    opens io.github.mgrtomaszzurawski.ksef.xml.auth to jakarta.xml.bind;
}
