/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionCooldownException;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the post-termination cooldown detection wired into
 * {@link KsefClient#openSession(FormCode)}.
 *
 * <p>After {@code POST /sessions/online} returns 200 with a fresh reference
 * number, {@code KsefClient} immediately polls
 * {@code GET /sessions/{ref}}. If that status reports code
 * {@link KsefSessionCooldownException#COOLDOWN_STATUS_CODE 415} the SDK
 * throws {@link KsefSessionCooldownException} instead of returning a
 * session that would reject the first {@code send(...)} call.
 *
 * <p>The empirical cooldown was discovered against live KSeF demo
 * (Codex 2026-05-05 #8b) — this test pins the SDK-side detection so a
 * future refactor cannot silently regress the proactive guard.
 */
@WireMockTest
class KsefSessionCooldownDetectionTest {

    private static final String ONLINE_PATH = "/v2/sessions/online";
    private static final String SESSION_REF = "20260507-SE-1234567890-CDCDCDCDCD-01";
    private static final String STATUS_PATH = "/v2/sessions/" + SESSION_REF;
    private static final String SECURITY_KEYS_PATH = "/v2/security/public-key-certificates";

    private static final String OPEN_ONLINE_RESPONSE = """
            {
              "referenceNumber": "%s",
              "validUntil": "2026-05-07T13:00:00+02:00"
            }
            """.formatted(SESSION_REF);

    private static final String COOLDOWN_STATUS_RESPONSE = """
            {
              "status": {
                "code": 415,
                "description": "Session in cooldown after recent termination"
              },
              "dateCreated": "2026-05-07T12:00:00+02:00"
            }
            """;

    @Test
    void openSession_whenStatusReports415_throwsCooldownException(WireMockRuntimeInfo wmInfo) {
        // given — full auth flow + symmetric key cert + open returns OK + status returns 415
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubFor(post(urlEqualTo(ONLINE_PATH))
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                    TestHttpConstants.APPLICATION_JSON)
                            .withBody(OPEN_ONLINE_RESPONSE)));
            stubFor(get(urlEqualTo(STATUS_PATH))
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                    TestHttpConstants.APPLICATION_JSON)
                            .withBody(COOLDOWN_STATUS_RESPONSE)));

            // when / then
            KsefSessionCooldownException cooldown =
                    assertThrows(KsefSessionCooldownException.class,
                            () -> client.invoices().openSession(FormCode.FA3));

            assertEquals(KsefSessionCooldownException.TYPICAL_COOLDOWN, cooldown.suggestedRetryAfter());
            // Wire-shape pin: both calls of the cooldown detection sequence
            // (open + proactive status poll) MUST have hit the server. A
            // future refactor that drops either step would silently regress
            // the proactive guard — verify catches that.
            verify(postRequestedFor(urlEqualTo(ONLINE_PATH)));
            verify(getRequestedFor(urlEqualTo(STATUS_PATH)));
        }
    }

    private static void stubSymmetricKeyEncryptionCert() {
        // KsefAuthFlowFixture stubs /security/public-key-certificates with a single
        // KsefTokenEncryption cert. openSession needs SymmetricKeyEncryption — override
        // at priority 1 so this stub wins over the fixture's default.
        try {
            TestCertificates testCerts = TestCertificates.generateRsa();
            String certBase64 = Base64.getEncoder().encodeToString(testCerts.certificate().getEncoded());
            String validFrom = OffsetDateTime.now().toString();
            String later = OffsetDateTime.now().plusYears(1).toString();
            stubFor(get(urlEqualTo(SECURITY_KEYS_PATH))
                    .atPriority(1)
                    .willReturn(aResponse()
                            .withStatus(TestHttpConstants.HTTP_OK)
                            .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER,
                                    TestHttpConstants.APPLICATION_JSON)
                            .withBody("""
                                    [
                                      {
                                        "certificate": "%s",
                                        "usage": ["KsefTokenEncryption"],
                                        "validFrom": "%s",
                                        "validTo": "%s"
                                      },
                                      {
                                        "certificate": "%s",
                                        "usage": ["SymmetricKeyEncryption"],
                                        "validFrom": "%s",
                                        "validTo": "%s"
                                      }
                                    ]
                                    """.formatted(certBase64, validFrom, later,
                                    certBase64, validFrom, later))));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to stub SymmetricKeyEncryption cert", ex);
        }
    }
}
