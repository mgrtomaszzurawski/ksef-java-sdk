/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.common.mapping.CommonMappers;

/**
 * Client for KSeF security operations (public key certificate retrieval).
 * These operations do not require authentication.
 *
 * @since 1.0.0
 */
public final class SecurityClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityClient.class);
    private static final String LOG_CALL = "→ {}";

    private static final String PATH_PUBLIC_KEY_CERTS = ApiPaths.SECURITY + "/public-key-certificates";
    private static final String OP_GET_PUBLIC_KEYS = "getPublicKeyCertificates";

    private final HttpSupport http;

    public SecurityClient(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Retrieve the list of public key certificates used for encrypting tokens
     * and symmetric keys when communicating with KSeF.
     *
     * @return list of public key certificates with validity periods and usage types
     */
    public List<PublicKeyCertificate> getPublicKeyCertificates() {
        LOGGER.debug(LOG_CALL, OP_GET_PUBLIC_KEYS);
        List<PublicKeyCertificateRaw> rawList = http.getList(PATH_PUBLIC_KEY_CERTS,
                new TypeReference<>() {}, OP_GET_PUBLIC_KEYS);
        return rawList.stream().map(CommonMappers::toPublicKeyCertificate).toList();
    }
}
