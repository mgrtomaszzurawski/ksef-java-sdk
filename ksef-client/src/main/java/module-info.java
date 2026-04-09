/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

module io.github.mgrtomaszzurawski.ksef {

    // SDK public API
    exports io.github.mgrtomaszzurawski.ksef.sdk;
    exports io.github.mgrtomaszzurawski.ksef.sdk.exception;
    exports io.github.mgrtomaszzurawski.ksef.sdk.model;
    exports io.github.mgrtomaszzurawski.ksef.sdk.model.builder;
    exports io.github.mgrtomaszzurawski.ksef.sdk.paging;
    exports io.github.mgrtomaszzurawski.ksef.sdk.crypto;
    exports io.github.mgrtomaszzurawski.ksef.sdk.signing;
    exports io.github.mgrtomaszzurawski.ksef.sdk.http;

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
