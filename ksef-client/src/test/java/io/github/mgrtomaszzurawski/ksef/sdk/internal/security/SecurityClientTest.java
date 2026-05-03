/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.security;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class SecurityClientTest {

    private static final String PATH_PUBLIC_KEY_CERTS = "/v2/security/public-key-certificates";
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
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(PUBLIC_KEY_CERTS_RESPONSE)));

        try (KsefClient ksef = createClient(wmInfo)) {
            SecurityClient securityClient = new SecurityClient(ksef.runtime());

            // when
            List<PublicKeyCertificate> certs = securityClient.getPublicKeyCertificates();

            // then
            assertFalse(certs.isEmpty());
            assertEquals(1, certs.size());
        }
    }

    @Test
    void getPublicKeyCertificates_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        try (KsefClient ksef = createClient(wmInfo)) {
            SecurityClient securityClient = new SecurityClient(ksef.runtime());

            // then
            assertThrows(KsefServerException.class, securityClient::getPublicKeyCertificates);
        }
    }

    private static KsefClient createClient(WireMockRuntimeInfo wmInfo) {
        return KsefClient.builder(KsefEnvironment.custom(wmInfo.getHttpBaseUrl() + "/v2"))
                .credentials(new KsefTokenCredentials("test-token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().enabled(false).build())
                .build();
    }
}
