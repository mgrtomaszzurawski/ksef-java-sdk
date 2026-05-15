/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth;

import io.github.mgrtomaszzurawski.ksef.sdk.config.AuthorizationPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.CertificateSubjectIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import org.jspecify.annotations.Nullable;

/**
 * What is being authenticated — the request side of a KSeF XAdES auth
 * exchange. Bundled so {@code AuthClient.authenticateWithXades(...)}
 * reads as "request + signing material" rather than carrying eight
 * loose parameters through the call chain.
 *
 * <p>Internal type — consumers do not construct this directly; the
 * facade builds it inside {@code KsefClient.authenticateWithCertificate}.
 *
 * @since 1.0.0
 */
public record XadesAuthRequest(
        String challenge,
        KsefIdentifier identifier,
        CertificateSubjectIdentifier subjectIdentifier,
        @Nullable AuthorizationPolicy policy,
        @Nullable String defaultClientIp) {
}
