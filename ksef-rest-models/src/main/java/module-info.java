/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * OpenAPI-generated REST models (`*Raw` types) for the KSeF API v2.
 * Implementation detail — exported only to ksef-client (qualified export).
 * The {@code client.model} package is also opened to Jackson for
 * deserialization.
 */
module io.github.mgrtomaszzurawski.ksef.rest {

    requires transitive com.fasterxml.jackson.databind;
    requires transitive com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires transitive org.openapitools.jackson.nullable;
    requires transitive jakarta.annotation;
    requires java.net.http;

    exports io.github.mgrtomaszzurawski.ksef.client to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.client.api to io.github.mgrtomaszzurawski.ksef;
    exports io.github.mgrtomaszzurawski.ksef.client.model to io.github.mgrtomaszzurawski.ksef;

    opens io.github.mgrtomaszzurawski.ksef.client.model to com.fasterxml.jackson.databind;
}
