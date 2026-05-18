/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.security;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefServerException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.security.SecurityClient;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.KsefTestRuntime;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;

@WireMockTest
class SecurityClientTest {

    private static final String PATH_PUBLIC_KEY_CERTS = "/v2/security/public-key-certificates";

    @Test
    void getPublicKeyCertificates_whenServerReturnsData_returnsParsedCertificates(WireMockRuntimeInfo wmInfo) throws Exception {
        // given — a real DER-encoded self-signed cert so PublicKeyCertificate.from()
        // can parse it (PR8 made the factory typed; raw bytes that aren't valid
        // X.509 throw KsefCryptoException).
        TestCertificates rsaCertificate = TestCertificates.generateRsa();
        String certBase64 = Base64.getEncoder().encodeToString(rsaCertificate.certificate().getEncoded());
        String body = """
                [
                  {
                    "certificate": "%s",
                    "validFrom": "2026-01-01T00:00:00+01:00",
                    "validTo": "2027-01-01T00:00:00+01:00",
                    "usage": ["KsefTokenEncryption", "SymmetricKeyEncryption"]
                  }
                ]
                """.formatted(certBase64);
        stubFor(get(urlEqualTo(PATH_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(body)));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        SecurityClient securityClient = new SecurityClient(runtime);

        // when
        List<PublicKeyCertificate> certs = securityClient.getPublicKeyCertificates();

        // then — parsed X509 surface is reachable through the typed accessors.
        assertFalse(certs.isEmpty());
        assertEquals(1, certs.size());
        assertNotNull(certs.get(0).certificate());
        assertNotNull(certs.get(0).publicKey());
        assertEquals("CN=Test", certs.get(0).subjectName());
    }

    @Test
    void getPublicKeyCertificates_whenServerError_throwsServerException(WireMockRuntimeInfo wmInfo) {
        // given
        stubFor(get(urlEqualTo(PATH_PUBLIC_KEY_CERTS))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_SERVER_ERROR)
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        HttpRuntime runtime = KsefTestRuntime.forWireMock(wmInfo);
        SecurityClient securityClient = new SecurityClient(runtime);

        // then
        assertThrows(KsefServerException.class, securityClient::getPublicKeyCertificates);
    }
}
