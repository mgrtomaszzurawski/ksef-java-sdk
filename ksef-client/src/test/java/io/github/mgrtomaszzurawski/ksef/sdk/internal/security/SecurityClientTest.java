/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.security;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.authentication.KsefTokenCredentials;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
class SecurityClientTest {

    private static final String PATH_PUBLIC_KEY_CERTS = "/api/v2/security/public-key-certificates";
    private static final int HTTP_OK = 200;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final String PUBLIC_KEY_CERTS_RESPONSE = """
            [
              {
                "certificate": "dGVzdC1jZXJ0aWZpY2F0ZS1kYXRh",
                "validFrom": "2026-01-01T00:00:00+01:00",
                "validTo": "2027-01-01T00:00:00+01:00",
                "usage": ["KsefTokenEncryption", "SymmetricKeyEncryption"]
              }
            ]
            """;

    @Test
    void getPublicKeyCertificates_whenServerReturnsData_returnsParsedCertificates(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PUBLIC_KEY_CERTS_RESPONSE)));

        KsefClient ksef = createClient(wmInfo);
        SecurityClient securityClient = ksef.security();

        // when
        List<PublicKeyCertificate> certs = securityClient.getPublicKeyCertificates();

        // then
        assertFalse(certs.isEmpty());
        assertEquals(1, certs.size());
    }

    @Test
    void getPublicKeyCertificates_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(HTTP_SERVER_ERROR)
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        KsefClient ksef = createClient(wmInfo);
        SecurityClient securityClient = ksef.security();

        // then
        assertThrows(KsefServerException.class, securityClient::getPublicKeyCertificates);
    }

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl()))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }
}
