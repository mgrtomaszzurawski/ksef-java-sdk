/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

/**
 * Authentication credentials for KSeF API access.
 *
 * <p>Sealed interface with three implementations for different authentication methods:
 * <ul>
 *   <li>{@link KsefTokenCredentials} — KSeF token-based authentication</li>
 *   <li>{@link KsefCertificateCredentials} — certificate-based authentication (XAdES signature)</li>
 *   <li>{@link KsefPkcs12Credentials} — PKCS#12 keystore-based authentication</li>
 * </ul>
 *
 * @see KsefClient.Builder#credentials(KsefCredentials)
 */
public sealed interface KsefCredentials
        permits KsefTokenCredentials, KsefCertificateCredentials, KsefPkcs12Credentials {

    /**
     * The NIP (Polish tax identification number) of the authenticating entity.
     *
     * @return 10-digit NIP string
     */
    String nip();
}
