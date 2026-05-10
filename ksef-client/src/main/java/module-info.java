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

    // PR12b: typed Invoice accessors (Fa2Invoice.faktura(), PefInvoice.invoice(),
    // PefKorInvoice.creditNote()) currently return JAXB raw types. Exporting the
    // four root packages so JPMS consumers can use these accessors. Sub-packages
    // (xml.pef.cac, xml.pef.cbc, etc.) stay internal. Full SDK-record overlay
    // is tracked as PR21 (post-PR20 polish).
    exports io.github.mgrtomaszzurawski.ksef.xml.fa2;
    exports io.github.mgrtomaszzurawski.ksef.xml.fa3;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef.cac;
    exports io.github.mgrtomaszzurawski.ksef.xml.pef.cbc;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac;
    exports io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc;

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

    // Open JAXB-generated packages to jakarta.xml.bind for reflective marshalling
    opens io.github.mgrtomaszzurawski.ksef.xml.fa2;
    opens io.github.mgrtomaszzurawski.ksef.xml.fa3;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cac;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbc;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ext;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cacpl;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.cbcpl;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.ccts;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sig;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcac;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.sigcbc;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.udt;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades132;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xades141;
    opens io.github.mgrtomaszzurawski.ksef.xml.pef.xmldsig;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cac;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbc;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ext;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cacpl;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.cbcpl;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.ccts;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sig;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcac;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.sigcbc;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.udt;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades132;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xades141;
    opens io.github.mgrtomaszzurawski.ksef.xml.pefkor.xmldsig;
    opens io.github.mgrtomaszzurawski.ksef.xml.upo;
    opens io.github.mgrtomaszzurawski.ksef.xml.auth;
}
