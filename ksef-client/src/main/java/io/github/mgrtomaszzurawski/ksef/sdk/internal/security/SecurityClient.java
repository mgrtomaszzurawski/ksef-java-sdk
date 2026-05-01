/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.security;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.transport.HttpSupport;
import java.util.List;

/**
 * Client for KSeF security operations (public key certificate retrieval).
 * These operations do not require authentication.
 */
public final class SecurityClient {

    private static final String PATH_PUBLIC_KEY_CERTS = "/api/v2/security/public-key-certificates";
    private static final String OP_GET_PUBLIC_KEYS = "getPublicKeyCertificates";

    private final HttpSupport http;

    public SecurityClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
    }

    /**
     * Retrieve the list of public key certificates used for encrypting tokens
     * and symmetric keys when communicating with KSeF.
     *
     * @return list of public key certificates with validity periods and usage types
     */
    public List<PublicKeyCertificate> getPublicKeyCertificates() {
        List<PublicKeyCertificateRaw> rawList = http.getList(PATH_PUBLIC_KEY_CERTS,
                new TypeReference<>() {}, OP_GET_PUBLIC_KEYS);
        return rawList.stream().map(PublicKeyCertificate::from).toList();
    }
}
