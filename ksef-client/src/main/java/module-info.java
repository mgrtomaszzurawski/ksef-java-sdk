/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

module io.github.mgrtomaszzurawski.ksef {

    // SDK public API — entry point
    exports io.github.mgrtomaszzurawski.ksef.sdk;

    // Configuration: environments, identifiers, retry policy
    exports io.github.mgrtomaszzurawski.ksef.sdk.config;

    // Cross-domain envelope + value-object types (StatusInfo, KsefNumber)
    exports io.github.mgrtomaszzurawski.ksef.sdk.core;

    // Auth-session management accessor (KsefClient.authSessions())
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.auth;

    // Authentication public DTOs (AuthSession returned by KsefClient.authSessions().streamAuthSessions(filter))
    exports io.github.mgrtomaszzurawski.ksef.sdk.domain.auth.model;

    // Invoicing functionality (online + batch sessions, invoice ops, QR, sync orchestrator)
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

    // ksef-xml-models owns xml.* — re-exported transitively so consumers
    // see the four root JAXB packages (FA2/FA3/PEF/PEF_KOR) via this SDK
    // dependency without declaring ksef-xml-models themselves (ADR-030).
    // UBL sub-package types (xml.ubl.cac / cbc) used by internal mappers
    // come in via qualified exports on the xml module.
    requires transitive io.github.mgrtomaszzurawski.ksef.xml;

    // ksef-rest-models owns the OpenAPI-generated client.* — internal use
    // only; not re-exported.
    requires io.github.mgrtomaszzurawski.ksef.rest;

    // Other module requirements
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
}
