/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.session;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefAuthFlowFixture;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.crypto.TestCertificates;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.TestHttpConstants;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the {@code OnlineSession.sendInvoice(...)} preflight contract:
 * the XSD validation gate must reject malformed / wrong-namespace /
 * structurally-invalid invoice XML <strong>before</strong> any wire
 * traffic reaches {@code POST /sessions/{ref}/invoices}.
 *
 * <p>Equivalent of {@code SubmitBatchFromFilesPreflightTest} for the
 * online-session write path. Regression guard: a future refactor that
 * drops the {@code InvoiceValidationGate.validate(...)} call from
 * {@code OnlineSessionImpl.send} would let broken XML reach KSeF,
 * burn quota, and surface as a generic server 400. This test pins
 * "zero wire calls on invalid input" so such regressions are caught.
 */
@WireMockTest
class OnlineSessionPreflightTest {

    private static final String SESSION_REF = "20260507-SE-1111111111-CDCDCDCDCD-01";
    private static final String ONLINE_OPEN_PATH = "/v2/sessions/online";
    private static final String STATUS_PATH = "/v2/sessions/" + SESSION_REF;
    private static final String SEND_INVOICE_PATH_REGEX = "/v2/sessions/" + SESSION_REF + "/invoices.*";
    private static final String SECURITY_KEYS_PATH = "/v2/security/public-key-certificates";

    private static final String OPEN_ONLINE_RESPONSE = """
            {
              "referenceNumber": "%s",
              "validUntil": "2026-05-07T13:00:00+02:00"
            }
            """.formatted(SESSION_REF);

    private static final String STATUS_OK_RESPONSE = """
            {
              "status": {
                "code": 200,
                "description": "Session ok"
              },
              "dateCreated": "2026-05-07T12:00:00+02:00"
            }
            """;

    private static final String UNRECOGNISED_ROOT_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><NotAnInvoice/>";
    private static final String FA3_STRUCTURALLY_INVALID_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<Faktura xmlns=\"http://crd.gov.pl/wzor/2025/06/25/13775/\">"
            + "<UnknownElement/></Faktura>";

    @Test
    void sendInvoice_whenInvoiceXmlIsWrongRoot_throwsBeforeAnyWireCall(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubSessionOpen();

            OnlineSession session = client.invoices().sessions().online(FormCode.FA3);
            Invoice invalid = Invoice.fromXml(FormCode.FA3, UNRECOGNISED_ROOT_XML.getBytes());

            assertThrows(KsefXmlValidator.KsefXmlValidationException.class,
                    () -> session.sendInvoice(invalid));

            assertEquals(0, findAll(postRequestedFor(urlMatching(SEND_INVOICE_PATH_REGEX))).size(),
                    "Validation gate must fail-fast before any POST to /sessions/{ref}/invoices");
        }
    }

    @Test
    void sendInvoice_whenInvoiceXmlIsXsdInvalid_throwsBeforeAnyWireCall(WireMockRuntimeInfo wmInfo) {
        try (KsefClient client = KsefAuthFlowFixture.newAuthenticatedClient(wmInfo)) {
            stubSymmetricKeyEncryptionCert();
            stubSessionOpen();

            OnlineSession session = client.invoices().sessions().online(FormCode.FA3);
            // Declares FA(3) namespace (passes root check) but body is structurally
            // invalid (no Naglowek/Podmiot1/Podmiot2/Fa) — XSD validation must reject.
            Invoice invalid = Invoice.fromXml(FormCode.FA3, FA3_STRUCTURALLY_INVALID_XML.getBytes());

            assertThrows(KsefXmlValidator.KsefXmlValidationException.class,
                    () -> session.sendInvoice(invalid));

            assertEquals(0, findAll(postRequestedFor(urlMatching(SEND_INVOICE_PATH_REGEX))).size(),
                    "XSD validation gate must fail-fast before any POST to /sessions/{ref}/invoices");
        }
    }

    private static void stubSessionOpen() {
        stubFor(post(urlEqualTo(ONLINE_OPEN_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(OPEN_ONLINE_RESPONSE)));
        stubFor(get(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(TestHttpConstants.HTTP_OK)
                        .withHeader(TestHttpConstants.CONTENT_TYPE_HEADER, TestHttpConstants.APPLICATION_JSON)
                        .withBody(STATUS_OK_RESPONSE)));
    }

    private static void stubSymmetricKeyEncryptionCert() {
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
