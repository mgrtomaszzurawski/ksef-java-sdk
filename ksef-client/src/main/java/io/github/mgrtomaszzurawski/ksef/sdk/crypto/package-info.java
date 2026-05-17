/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Cryptographic utilities exported as part of the public SDK surface.
 *
 * <p>Contains the {@link io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.KsefCryptoService}
 * (PKCS#8 private-key parsing, X.509 certificate parsing, RSA key-pair
 * generation, AES key/IV generation) and the
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator}
 * (XSD validation of FA(2) / FA(3) / PEF / RR invoice XML against the
 * bundled official KSeF schemas, with secure-processing/XXE hardening).
 *
 * <p>Both are stateless thread-safe utilities — consumers may instantiate
 * once and share across threads.
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;
