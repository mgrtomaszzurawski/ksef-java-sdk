/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.time.OffsetDateTime;
import java.util.Base64;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Test-only fixture that drives the full KSeF token-based authentication
 * flow against a WireMock instance.
 *
 * <p>Used by SDK tests that need an authenticated {@link KsefClient}
 * without poking internals. Per ADR-020 the SDK exposes no test seam
 * for skipping auth, so tests drive the real challenge → encrypt-token
 * → poll → redeem flow against WireMock stubs.
 *
 * <p>Stubs the five auth endpoints plus the
 * {@code /security/public-key-certificates} endpoint that the SDK
 * fetches to obtain the token-encryption RSA public key. Uses a
 * deterministic test RSA key from {@link TestCertificates#generateRsa()}
 * — generated once per fixture instance.
 *
 * <p>Returns an authenticated {@link KsefClient} ready for use. The
 * configured access token is the {@code accessToken} parameter passed
 * to {@link #newAuthenticatedClient}.
 */
public final class KsefAuthFlowFixture {

    public static final String DEFAULT_TEST_TOKEN = "test-access-token";
    public static final String DEFAULT_TEST_NIP = "1111111111";
    public static final String DEFAULT_TEST_REFERENCE = "20260404-AU-1111111111-ABCDEF1234-01";
    public static final String DEFAULT_TEST_KSEF_TOKEN = "test-ksef-token";

    private static final String AUTH_PREFIX = "/v2/auth";
    private static final String SECURITY_PUBLIC_KEY_CERTS = "/v2/security/public-key-certificates";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private KsefAuthFlowFixture() { }

    /**
     * Stub the full token auth flow on the supplied WireMock instance,
     * then build and return a {@link KsefClient} configured with token
     * credentials whose first protected call will trigger lazy auth and
     * end up holding {@code accessToken} as its session token.
     *
     * <p>Caller closes the returned client (try-with-resources). The
     * client uses {@link RetryPolicy#enabled} = false to keep test
     * timing predictable; pass a non-default policy via
     * {@link #newAuthenticatedClient(WireMockRuntimeInfo, String, String, RetryPolicy)}
     * if needed.
     */
    public static KsefClient newAuthenticatedClient(WireMockRuntimeInfo wmInfo) {
        return newAuthenticatedClient(wmInfo, DEFAULT_TEST_TOKEN, DEFAULT_TEST_NIP,
                RetryPolicy.builder().enabled(false).build());
    }

    public static KsefClient newAuthenticatedClient(WireMockRuntimeInfo wmInfo, String accessToken, String nip) {
        return newAuthenticatedClient(wmInfo, accessToken, nip,
                RetryPolicy.builder().enabled(false).build());
    }

    public static KsefClient newAuthenticatedClient(WireMockRuntimeInfo wmInfo,
                                                     String accessToken,
                                                     String nip,
                                                     RetryPolicy retryPolicy) {
        TestCertificates certs;
        try {
            certs = TestCertificates.generateRsa();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate test RSA keypair for auth fixture", ex);
        }
        stubAllAuthEndpoints(accessToken, certs);
        return KsefClient.builder().environment(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials(DEFAULT_TEST_KSEF_TOKEN, nip))
                .retryPolicy(retryPolicy)
                .build();
    }

    private static void stubAllAuthEndpoints(String accessToken, TestCertificates certs) {
        String certPemBody = encodeCertAsPem(certs);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime later = now.plusHours(1);

        // /security/public-key-certificates — SDK fetches before encrypting the
        // KSeF token. Returns one cert with usage KsefTokenEncryption.
        stubFor(get(urlEqualTo(SECURITY_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody("""
                                [{
                                  "certificate": "%s",
                                  "usage": ["KsefTokenEncryption"],
                                  "validFrom": "%s",
                                  "validTo": "%s"
                                }]
                                """.formatted(certPemBody, now.toString(), later.toString()))));

        // /auth/challenge — POST returns challenge + timestamp + clientIp
        stubFor(post(urlEqualTo(AUTH_PREFIX + "/challenge"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody("""
                                {
                                  "challenge": "test-challenge",
                                  "timestamp": "%s",
                                  "timestampMs": %d,
                                  "clientIp": "127.0.0.1"
                                }
                                """.formatted(now.toString(), now.toInstant().toEpochMilli()))));

        // /auth/ksef-token — POST returns referenceNumber + authenticationToken
        stubFor(post(urlEqualTo(AUTH_PREFIX + "/ksef-token"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody("""
                                {
                                  "referenceNumber": "%s",
                                  "authenticationToken": {
                                    "token": "test-operation-token",
                                    "validUntil": "%s"
                                  }
                                }
                                """.formatted(DEFAULT_TEST_REFERENCE, later.toString()))));

        // /auth/{ref} — GET returns status 200 (auth complete)
        stubFor(get(urlMatching(AUTH_PREFIX + "/[^/]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody("""
                                {
                                  "status": {"code": 200, "description": "Authenticated"}
                                }
                                """)));

        // /auth/token/redeem — POST returns access + refresh tokens
        stubFor(post(urlEqualTo(AUTH_PREFIX + "/token/redeem"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .withBody("""
                                {
                                  "accessToken": {
                                    "token": "%s",
                                    "validUntil": "%s"
                                  },
                                  "refreshToken": {
                                    "token": "test-refresh-token",
                                    "validUntil": "%s"
                                  }
                                }
                                """.formatted(accessToken, later.toString(), later.plusDays(7).toString()))));
    }

    private static String encodeCertAsPem(TestCertificates certs) {
        try {
            byte[] derBytes = certs.certificate().getEncoded();
            return Base64.getEncoder().encodeToString(derBytes);
        } catch (java.security.cert.CertificateEncodingException ex) {
            throw new IllegalStateException("Failed to encode test cert", ex);
        }
    }
}
