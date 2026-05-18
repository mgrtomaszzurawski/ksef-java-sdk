/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Cryptographic utilities exported as part of the public SDK surface.
 *
 * <p>Contains {@link io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator}
 * (XSD validation of FA(2) / FA(3) / PEF / RR invoice XML against the
 * bundled official KSeF schemas, with secure-processing/XXE hardening)
 * and the {@link io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrRequest} /
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrResult} CSR-flow
 * value records. The internal AES/RSA key + IV generation, PKCS#8 / X.509
 * parsing, and certificate-handling utilities live under
 * {@code sdk.internal.runtime.crypto} (R1-5) — they are not part of the
 * consumer surface.
 *
 * <p>The exported utilities are stateless and thread-safe — consumers may
 * instantiate once and share across threads.
 *
 * @since 1.0.0
 */
@org.jspecify.annotations.NullMarked
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;
