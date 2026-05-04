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
    private static final String CERTIFICATE_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA";
    private static final int LIMIT_VALUE = 12;
    private static final int LIMIT_REMAINING = 6;

    @Test
    void toCertificateLimit_mapsAllFields() throws Exception {
        String json = """
                {"remaining": %d, "limit": %d}
                """.formatted(LIMIT_REMAINING, LIMIT_VALUE);
        CertificateLimitRaw raw = OBJECT_MAPPER.readValue(json, CertificateLimitRaw.class);

        CertificateLimit result = CertificatesMappers.toCertificateLimit(raw);

        assertEquals(LIMIT_REMAINING, result.remaining());
        assertEquals(LIMIT_VALUE, result.limit());
    }

    @Test
    void toCertificateLimit_nullInput_yieldsNull() {
        assertNull(CertificatesMappers.toCertificateLimit(null));
    }

    @Test
    void toCertificateLimits_mapsBothNestedLimits() throws Exception {
        String json = """
                {
                  "canRequest": true,
                  "enrollment": {"remaining": %d, "limit": %d},
                  "certificate": {"remaining": %d, "limit": %d}
                }
                """.formatted(LIMIT_REMAINING, LIMIT_VALUE, LIMIT_REMAINING, LIMIT_VALUE);
        CertificateLimitsResponseRaw raw = OBJECT_MAPPER.readValue(json, CertificateLimitsResponseRaw.class);

        CertificateLimits result = CertificatesMappers.toCertificateLimits(raw);

        assertNotNull(result.enrollment());
        assertNotNull(result.certificate());
        assertEquals(LIMIT_REMAINING, result.enrollment().remaining());
    }

    @Test
    void toCertificateEnrollmentData_mapsAllFields() throws Exception {
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

        CertificateEnrollmentData result = CertificatesMappers.toCertificateEnrollmentData(raw);

        assertEquals(COMMON_NAME, result.commonName());
        assertEquals("PL", result.countryName());
        assertEquals("Jan", result.givenName());
        assertEquals("Kowalski", result.surname());
        assertEquals(SERIAL, result.serialNumber());
    }

    @Test
    void toCertificateEnrollmentStatus_mapsAllFields() throws Exception {
        String json = """
                {
                  "requestDate": "%s",
                  "status": {"code": 200, "description": "OK"},
                  "certificateSerialNumber": "%s"
                }
                """.formatted(DATE_ISO, SERIAL);
        CertificateEnrollmentStatusResponseRaw raw =
                OBJECT_MAPPER.readValue(json, CertificateEnrollmentStatusResponseRaw.class);

        CertificateEnrollmentStatus result = CertificatesMappers.toCertificateEnrollmentStatus(raw);

        assertEquals(SERIAL, result.certificateSerialNumber());
        assertNotNull(result.status());
        assertEquals(200, result.status().code());
    }

    @Test
    void toCertificateListItem_withSubjectIdentifier_mapsAllFields() throws Exception {
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

        CertificateListItem result = CertificatesMappers.toCertificateListItem(raw);

        assertEquals(SERIAL, result.certificateSerialNumber());
        assertEquals(NAME, result.name());
        assertEquals("Authentication", result.type());
        assertEquals("Active", result.status());
        assertEquals("Nip", result.subjectIdentifierType());
        assertEquals(NIP, result.subjectIdentifierValue());
    }

    @Test
    void toCertificateListItem_withoutSubjectIdentifier_yieldsNullSubjectFields() throws Exception {
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

        CertificateListItem result = CertificatesMappers.toCertificateListItem(raw);

        assertNull(result.subjectIdentifierType());
        assertNull(result.subjectIdentifierValue());
        assertEquals("Offline", result.type());
        assertEquals("Expired", result.status());
    }

    @Test
    void toEnrollCertificateResult_mapsAllFields() throws Exception {
        String json = """
                {"referenceNumber": "ref-1", "timestamp": "%s"}
                """.formatted(DATE_ISO);
        EnrollCertificateResponseRaw raw = OBJECT_MAPPER.readValue(json, EnrollCertificateResponseRaw.class);

        EnrollCertificateResult result = CertificatesMappers.toEnrollCertificateResult(raw);

        assertEquals("ref-1", result.referenceNumber());
        assertNotNull(result.timestamp());
    }

    @Test
    void toRetrievedCertificate_mapsAllFields() throws Exception {
        String json = """
                {
                  "certificate": "%s",
                  "certificateName": "%s",
                  "certificateSerialNumber": "%s",
                  "certificateType": "Authentication"
                }
                """.formatted(CERTIFICATE_BASE64, NAME, SERIAL);
        RetrieveCertificatesListItemRaw raw = OBJECT_MAPPER.readValue(json, RetrieveCertificatesListItemRaw.class);

        RetrievedCertificate result = CertificatesMappers.toRetrievedCertificate(raw);

        assertNotNull(result.certificate());
        assertEquals(NAME, result.certificateName());
        assertEquals(SERIAL, result.certificateSerialNumber());
        assertEquals("Authentication", result.certificateType());
    }

    @Test
    void toCertificateRevocationReasonRaw_coversAllEnumBranches() {
        assertEquals(CertificateRevocationReasonRaw.UNSPECIFIED,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.UNSPECIFIED));
        assertEquals(CertificateRevocationReasonRaw.SUPERSEDED,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.SUPERSEDED));
        assertEquals(CertificateRevocationReasonRaw.KEY_COMPROMISE,
                CertificatesMappers.toCertificateRevocationReasonRaw(CertificateRevocationReason.KEY_COMPROMISE));
    }

    @Test
    void toCertificateListItemStatusRaw_coversAllEnumBranches() {
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
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION,
                CertificatesMappers.toKsefCertificateTypeRaw(KsefCertificateType.AUTHENTICATION));
        assertEquals(KsefCertificateTypeRaw.OFFLINE,
                CertificatesMappers.toKsefCertificateTypeRaw(KsefCertificateType.OFFLINE));
    }

    @Test
    void toEnrollCertificateRequestRaw_withValidFrom_setsField() {
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest(
                NAME, KsefCertificateType.AUTHENTICATION, CSR_BYTES,
                java.time.OffsetDateTime.parse(DATE_ISO));

        var raw = CertificatesMappers.toEnrollCertificateRequestRaw(request);

        assertEquals(NAME, raw.getCertificateName());
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION, raw.getCertificateType());
        assertNotNull(raw.getCsr());
        assertNotNull(raw.getValidFrom());
    }

    @Test
    void toEnrollCertificateRequestRaw_withoutValidFrom_skipsValidFrom() {
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest(
                NAME, KsefCertificateType.OFFLINE, CSR_BYTES, null);

        var raw = CertificatesMappers.toEnrollCertificateRequestRaw(request);

        assertEquals(NAME, raw.getCertificateName());
        assertEquals(KsefCertificateTypeRaw.OFFLINE, raw.getCertificateType());
        assertNull(raw.getValidFrom());
    }

    @Test
    void toQueryCertificatesRequestRaw_setsAllOptionalFields() {
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest(
                SERIAL, NAME, KsefCertificateType.AUTHENTICATION,
                CertificateStatus.ACTIVE, java.time.OffsetDateTime.parse(DATE_ISO));

        var raw = CertificatesMappers.toQueryCertificatesRequestRaw(request);

        assertEquals(SERIAL, raw.getCertificateSerialNumber());
        assertEquals(NAME, raw.getName());
        assertEquals(KsefCertificateTypeRaw.AUTHENTICATION, raw.getType());
        assertEquals(CertificateListItemStatusRaw.ACTIVE, raw.getStatus());
        assertNotNull(raw.getExpiresAfter());
    }

    @Test
    void toQueryCertificatesRequestRaw_skipsNullFields() {
        var request = new io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest(
                null, null, null, null, null);

        var raw = CertificatesMappers.toQueryCertificatesRequestRaw(request);

        assertNull(raw.getName());
        assertNull(raw.getType());
        assertNull(raw.getStatus());
        assertNull(raw.getExpiresAfter());
    }
}
