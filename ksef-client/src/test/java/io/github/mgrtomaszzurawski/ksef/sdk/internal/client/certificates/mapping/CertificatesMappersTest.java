/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateLimitsResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateListItemStatusRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateRevocationReasonRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RetrieveCertificatesListItemRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimit;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateListItem;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.EnrollCertificateResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.RetrievedCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.crypto.TestCertificates;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Branch coverage for {@link CertificatesMappers} singular-record converters.
 *
 * <p>Existing {@code CertificateClientTest} stubs empty list responses, so the
 * per-element {@code toCertificateListItem} / {@code toRetrievedCertificate}
 * paths plus their nested enum/identifier branches stay at 0%. This suite
 * drives them directly via JSON-driven Raw construction.
 */
class CertificatesMappersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());

    private static final String SERIAL = "01ABCDEF";
    private static final String NAME = "test-cert";
    private static final String COMMON_NAME = "CN=Test";
    private static final byte[] CSR_BYTES = new byte[]{0x30, 0x01, 0x02, 0x03};
    private static final String DATE_ISO = "2026-04-15T10:00:00+02:00";
    private static final String NIP = "1234567890";
    // Real DER bytes generated lazily — PR8 made RetrievedCertificate.from()
    // require a parseable X.509 (typed surface).
    private static final String CERTIFICATE_BASE64 = realCertificateBase64();

    private static String realCertificateBase64() {
        try {
            return Base64.getEncoder().encodeToString(
                    TestCertificates.generateRsa().certificate().getEncoded());
        } catch (Exception generationFailure) {
            throw new IllegalStateException("Could not generate test certificate", generationFailure);
        }
    }
    private static final int LIMIT_VALUE = 12;
    private static final int LIMIT_REMAINING = 6;

    @Test
    void toCertificateLimit_mapsAllFields() throws Exception {
        // given
        String json = """
                {"remaining": %d, "limit": %d}
                """.formatted(LIMIT_REMAINING, LIMIT_VALUE);
        CertificateLimitRaw raw = OBJECT_MAPPER.readValue(json, CertificateLimitRaw.class);

        // when
        CertificateLimit result = CertificatesMappers.toCertificateLimit(raw);

        // then
        assertEquals(LIMIT_REMAINING, result.remaining());
        assertEquals(LIMIT_VALUE, result.limit());
    }

    @Test
    void toCertificateLimit_nullInput_yieldsNull() {
        // when / then
        assertNull(CertificatesMappers.toCertificateLimit(null));
    }

    @Test
    void toCertificateLimits_mapsBothNestedLimits() throws Exception {
        // given
        String json = """
                {
                  "canRequest": true,
                  "enrollment": {"remaining": %d, "limit": %d},
                  "certificate": {"remaining": %d, "limit": %d}
                }
                """.formatted(LIMIT_REMAINING, LIMIT_VALUE, LIMIT_REMAINING, LIMIT_VALUE);
        CertificateLimitsResponseRaw raw = OBJECT_MAPPER.readValue(json, CertificateLimitsResponseRaw.class);

        // when
        CertificateLimits result = CertificatesMappers.toCertificateLimits(raw);

        // then
        assertNotNull(result.enrollment());
        assertNotNull(result.certificate());
        assertEquals(LIMIT_REMAINING, result.enrollment().remaining());
    }

    @Test
    void toCertificateEnrollmentData_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "commonName": "%s",
                  "countryName": "PL",
                  "givenName": "Jan",
                  "surname": "Kowalski",
                  "organizationName": "Acme",
                  "organizationIdentifier": "VATPL-1234567890",
                  "serialNumber": "%s",
                  "uniqueIdentifier": "U-1"
                }
                """.formatted(COMMON_NAME, SERIAL);
        CertificateEnrollmentDataResponseRaw raw =
                OBJECT_MAPPER.readValue(json, CertificateEnrollmentDataResponseRaw.class);

        // when
        CertificateEnrollmentData result = CertificatesMappers.toCertificateEnrollmentData(raw);

        // then
        assertEquals(COMMON_NAME, result.commonName());
        assertEquals("PL", result.countryName());
        assertEquals("Jan", result.givenName());
        assertEquals("Kowalski", result.surname());
        assertEquals(SERIAL, result.serialNumber());
    }

    @Test
    void toCertificateEnrollmentStatus_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "requestDate": "%s",
                  "status": {"code": 200, "description": "OK"},
                  "certificateSerialNumber": "%s"
                }
                """.formatted(DATE_ISO, SERIAL);
        CertificateEnrollmentStatusResponseRaw raw =
                OBJECT_MAPPER.readValue(json, CertificateEnrollmentStatusResponseRaw.class);

        // when
        CertificateEnrollmentStatus result = CertificatesMappers.toCertificateEnrollmentStatus(raw);

        // then
        assertEquals(SERIAL, result.certificateSerialNumber());
        assertNotNull(result.status());
        assertEquals(200, result.status().code());
    }

    @Test
    void toCertificateListItem_withSubjectIdentifier_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "certificateSerialNumber": "%s",
                  "name": "%s",
                  "type": "Authentication",
                  "commonName": "%s",
                  "status": "Active",
                  "subjectIdentifier": {"type": "Nip", "value": "%s"},
                  "validFrom": "%s",
                  "validTo": "%s",
                  "lastUseDate": "%s",
                  "requestDate": "%s"
                }
                """.formatted(SERIAL, NAME, COMMON_NAME, NIP, DATE_ISO, DATE_ISO, DATE_ISO, DATE_ISO);
        CertificateListItemRaw raw = OBJECT_MAPPER.readValue(json, CertificateListItemRaw.class);

        // when
        CertificateListItem result = CertificatesMappers.toCertificateListItem(raw);

        // then
        assertEquals(SERIAL, result.certificateSerialNumber());
        assertEquals(NAME, result.name());
        assertEquals("Authentication", result.type());
        assertEquals("Active", result.status());
        assertEquals("Nip", result.subjectIdentifierType());
        assertEquals(NIP, result.subjectIdentifierValue());
    }

    @Test
    void toCertificateListItem_withoutSubjectIdentifier_yieldsNullSubjectFields() throws Exception {
        // given
        String json = """
                {
                  "certificateSerialNumber": "%s",
                  "name": "%s",
                  "type": "Offline",
                  "commonName": "%s",
                  "status": "Expired",
                  "subjectIdentifier": null,
                  "validFrom": "%s",
                  "validTo": "%s",
                  "lastUseDate": "%s",
                  "requestDate": "%s"
                }
                """.formatted(SERIAL, NAME, COMMON_NAME, DATE_ISO, DATE_ISO, DATE_ISO, DATE_ISO);
        CertificateListItemRaw raw = OBJECT_MAPPER.readValue(json, CertificateListItemRaw.class);

        // when
        CertificateListItem result = CertificatesMappers.toCertificateListItem(raw);

        // then
        assertNull(result.subjectIdentifierType());
        assertNull(result.subjectIdentifierValue());
        assertEquals("Offline", result.type());
        assertEquals("Expired", result.status());
    }

    @Test
    void toEnrollCertificateResult_mapsAllFields() throws Exception {
        // given
        String json = """
                {"referenceNumber": "ref-1", "timestamp": "%s"}
                """.formatted(DATE_ISO);
        EnrollCertificateResponseRaw raw = OBJECT_MAPPER.readValue(json, EnrollCertificateResponseRaw.class);

        // when
        EnrollCertificateResult result = CertificatesMappers.toEnrollCertificateResult(raw);

        // then
        assertEquals("ref-1", result.referenceNumber());
        assertNotNull(result.timestamp());
    }

    @Test
    void toRetrievedCertificate_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "certificate": "%s",
                  "certificateName": "%s",
                  "certificateSerialNumber": "%s",
                  "certificateType": "Authentication"
                }
                """.formatted(CERTIFICATE_BASE64, NAME, SERIAL);
        RetrieveCertificatesListItemRaw raw = OBJECT_MAPPER.readValue(json, RetrieveCertificatesListItemRaw.class);

        // when
        RetrievedCertificate result = CertificatesMappers.toRetrievedCertificate(raw);

        // then — typed accessors exposed on the new RetrievedCertificate surface.
        assertNotNull(result.certificate());
        assertNotNull(result.publicKey());
        assertNotNull(result.der());
        assertEquals(NAME, result.certificateName());
        assertEquals(SERIAL, result.certificateSerialNumber());
        assertEquals(KsefCertificateType.AUTHENTICATION, result.certificateType());
    }

    @Test
    void toCertificateRevocationReasonRaw_coversAllEnumBranches() {
        // when / then
        assertEquals(CertificateRevocationReasonRaw.UNSPECIFIED,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.UNSPECIFIED));
        assertEquals(CertificateRevocationReasonRaw.SUPERSEDED,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.SUPERSEDED));
        assertEquals(CertificateRevocationReasonRaw.KEY_COMPROMISE,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.KEY_COMPROMISE));
    }

    @Test
    void toCertificateListItemStatusRaw_coversAllEnumBranches() {
        // when / then
        assertEquals(CertificateListItemStatusRaw.ACTIVE,
                CertificatesMappers.toCertificateListItemStatusRaw(CertificateStatus.ACTIVE));
        assertEquals(CertificateListItemStatusRaw.BLOCKED,
                CertificatesMappers.toCertificateListItemStatusRaw(CertificateStatus.BLOCKED));
        assertEquals(CertificateListItemStatusRaw.REVOKED,
                CertificatesMappers.toCertificateListItemStatusRaw(CertificateStatus.REVOKED));
        assertEquals(CertificateListItemStatusRaw.EXPIRED,
                CertificatesMappers.toCertificateListItemStatusRaw(CertificateStatus.EXPIRED));
    }

    @Test
    void toKsefCertificateTypeRaw_coversAllEnumBranches() {
        // when / then
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION,
                CertificatesMappers.toKsefCertificateTypeRaw(KsefCertificateType.AUTHENTICATION));
        assertEquals(KsefCertificateTypeRaw.OFFLINE,
                CertificatesMappers.toKsefCertificateTypeRaw(KsefCertificateType.OFFLINE));
    }

    @Test
    void toEnrollCertificateRequestRaw_withValidFrom_setsField() {
        // given
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest(
                NAME, KsefCertificateType.AUTHENTICATION, CSR_BYTES,
                java.time.OffsetDateTime.parse(DATE_ISO));

        // when
        var rawRequest = CertificatesMappers.toEnrollCertificateRequestRaw(request);

        // then
        assertEquals(NAME, rawRequest.getCertificateName());
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION, rawRequest.getCertificateType());
        assertNotNull(rawRequest.getCsr());
        assertNotNull(rawRequest.getValidFrom());
    }

    @Test
    void toEnrollCertificateRequestRaw_withoutValidFrom_skipsValidFrom() {
        // given
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest(
                NAME, KsefCertificateType.OFFLINE, CSR_BYTES, null);

        // when
        var rawRequest = CertificatesMappers.toEnrollCertificateRequestRaw(request);

        // then
        assertEquals(NAME, rawRequest.getCertificateName());
        assertEquals(KsefCertificateTypeRaw.OFFLINE, rawRequest.getCertificateType());
        assertNull(rawRequest.getValidFrom());
    }

    @Test
    void toQueryCertificatesRequestRaw_setsAllOptionalFields() {
        // given
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest(
                SERIAL, NAME, KsefCertificateType.AUTHENTICATION,
                CertificateStatus.ACTIVE, java.time.OffsetDateTime.parse(DATE_ISO), null, null);

        // when
        var rawRequest = CertificatesMappers.toQueryCertificatesRequestRaw(request);

        // then
        assertEquals(SERIAL, rawRequest.getCertificateSerialNumber());
        assertEquals(NAME, rawRequest.getName());
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION, rawRequest.getType());
        assertEquals(CertificateListItemStatusRaw.ACTIVE, rawRequest.getStatus());
        assertNotNull(rawRequest.getExpiresAfter());
    }

    @Test
    void toQueryCertificatesRequestRaw_skipsNullFields() {
        // given
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest(
                null, null, null, null, null, null, null);

        // when
        var rawRequest = CertificatesMappers.toQueryCertificatesRequestRaw(request);

        // then
        assertNull(rawRequest.getName());
        assertNull(rawRequest.getType());
        assertNull(rawRequest.getStatus());
        assertNull(rawRequest.getExpiresAfter());
    }
}
