/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

module io.github.mgrtomaszzurawski.ksef {

    // SDK public API — entry point
    exports io.github.mgrtomaszzurawski.ksef.sdk;

    // Configuration: environments, identifiers, retry policy
    exports io.github.mgrtomaszzurawski.ksef.sdk.config;

    // Authentication: credentials and auth-flow models
    exports io.github.mgrtomaszzurawski.ksef.sdk.authentication;

    // Common shared types (StatusInfo, TokenInfo, public-key models)
    exports io.github.mgrtomaszzurawski.ksef.sdk.common;

    // Invoicing functionality (online + batch sessions, invoice ops, QR, batch helper)
    exports io.github.mgrtomaszzurawski.ksef.sdk.invoicing;
    exports io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.invoicing.model;
    exports io.github.mgrtomaszzurawski.ksef.sdk.invoicing.batch;
    exports io.github.mgrtomaszzurawski.ksef.sdk.invoicing.qrcode;

    // Permissions functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.permissions;
    exports io.github.mgrtomaszzurawski.ksef.sdk.permissions.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.permissions.model;

    // Tokens functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.tokens;

    // Certificates functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.certificates;

    // Peppol functionality
    exports io.github.mgrtomaszzurawski.ksef.sdk.peppol;

    // Limits and rate-limit queries
    exports io.github.mgrtomaszzurawski.ksef.sdk.limits;

    // Test data functionality (api-test environment helpers)
    exports io.github.mgrtomaszzurawski.ksef.sdk.testdata;
    exports io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.testdata.model;

    // Exception hierarchy
    exports io.github.mgrtomaszzurawski.ksef.sdk.exception;

    // Internal mechanisms (sdk.internal.*) are NOT exported.

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
}
