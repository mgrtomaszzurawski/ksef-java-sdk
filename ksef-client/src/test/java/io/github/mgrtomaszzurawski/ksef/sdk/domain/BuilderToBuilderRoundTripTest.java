/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Round-trip tests: for each public builder, verify that
 * {@code original.toBuilder().build()} produces the same Raw output as
 * {@code original.build()} (or, for non-deterministic builders that generate
 * fresh AES keys / hashes, that the toBuilder()-produced Raw is non-null and
 * the same type).
 *
 * <p>Mirrors the toBuilder() contract documented per-builder. Builders not
 * listed here are non-deterministic (OnlineSessionBuilder, InvoiceExportBuilder,
 * SendInvoiceBuilder) and are covered by smoke assertions only.
 */
class BuilderToBuilderRoundTripTest {

    private static final String NIP = "1234567890";
    private static final String OTHER_NIP = "0987654321";
    private static final String PESEL = "12345678901";
    private static final String FINGERPRINT = "ABC123DEF456";
    private static final String PEPPOL_ID = "PL1234567890";
    private static final String VAT_UE = "PL1234567890";
    private static final String DESCRIPTION = "Round-trip test description";
    private static final String FIRST_NAME = "Jan";
    private static final String LAST_NAME = "Kowalski";
    private static final String FULL_NAME = "Test Sp. z o.o.";
    private static final String ADDRESS = "Warsaw, Poland";
    private static final String EU_ENTITY_NAME = "EU Partner GmbH";
    private static final String CERT_NAME = "Test Cert";
    private static final byte[] CSR = new byte[]{1, 2, 3, 4, 5};
    private static final String SUBUNIT_NAME = "Subunit A";

    private static final OffsetDateTime FIXED_DATE =
            OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void tokenGenerateBuilder_toBuilder_isEquivalent() {
        TokenGenerateBuilder original = TokenGenerateBuilder.create(DESCRIPTION).invoiceRead().credentialsRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
        assertNotSame(original, original.toBuilder());
    }

    @Test
    void certificateEnrollBuilder_toBuilder_isEquivalent() {
        CertificateEnrollBuilder original = CertificateEnrollBuilder
                .create(CERT_NAME, KsefCertificateType.AUTHENTICATION, CSR)
                .validFrom(FIXED_DATE);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void certificateQueryBuilder_toBuilder_isEquivalent() {
        CertificateQueryBuilder original = CertificateQueryBuilder.create()
                .name(CERT_NAME)
                .type(KsefCertificateType.AUTHENTICATION)
                .status(CertificateStatus.ACTIVE)
                .expiresAfter(FIXED_DATE);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void invoiceQueryBuilder_toBuilder_isEquivalent() {
        InvoiceQueryBuilder original = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(FIXED_DATE)
                .dateTo(FIXED_DATE.plusMonths(1))
                .sellerNip(NIP);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void personPermissionGrantBuilder_toBuilder_isEquivalent() {
        PersonPermissionGrantBuilder original = PersonPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void entityPermissionGrantBuilder_toBuilder_isEquivalent() {
        EntityPermissionGrantBuilder original = EntityPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION)
                .entityDetails(FULL_NAME)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void entityAuthorizationPermissionGrantBuilder_toBuilder_isEquivalent() {
        EntityAuthorizationPermissionGrantBuilder original = EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION)
                .entityDetails(FULL_NAME)
                .selfInvoicing();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void indirectPermissionGrantBuilder_toBuilder_isEquivalent() {
        IndirectPermissionGrantBuilder original = IndirectPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME)
                .targetNip(OTHER_NIP)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void subunitPermissionGrantBuilder_toBuilder_isEquivalent() {
        SubunitPermissionGrantBuilder original = SubunitPermissionGrantBuilder.forPesel(PESEL)
                .contextNip(NIP)
                .description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void euEntityAdminPermissionGrantBuilder_toBuilder_isEquivalent() {
        EuEntityAdminPermissionGrantBuilder original = EuEntityAdminPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .contextNipVatUe(VAT_UE)
                .description(DESCRIPTION)
                .euEntityName(EU_ENTITY_NAME)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .euEntityDetails(EU_ENTITY_NAME, ADDRESS);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void euEntityPermissionGrantBuilder_toBuilder_isEquivalent() {
        EuEntityPermissionGrantBuilder original = EuEntityPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void personalPermissionsQueryBuilder_toBuilder_isEquivalent() {
        PersonalPermissionsQueryBuilder original = PersonalPermissionsQueryBuilder.create()
                .contextNip(NIP)
                .activeOnly()
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void personPermissionsQueryBuilder_toBuilder_isEquivalent() {
        PersonPermissionsQueryBuilder original = PersonPermissionsQueryBuilder.permissionsInCurrentContext()
                .authorizedByPesel(PESEL)
                .activeOnly()
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void entityAuthorizationPermissionsQueryBuilder_toBuilder_isEquivalent() {
        EntityAuthorizationPermissionsQueryBuilder original = EntityAuthorizationPermissionsQueryBuilder.granted()
                .authorizedByNip(NIP)
                .selfInvoicing();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void euEntityPermissionsQueryBuilder_toBuilder_isEquivalent() {
        EuEntityPermissionsQueryBuilder original = EuEntityPermissionsQueryBuilder.create()
                .vatUeIdentifier(VAT_UE)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testPermissionsGrantBuilder_toBuilder_isEquivalent() {
        TestPermissionsGrantBuilder original = TestPermissionsGrantBuilder.create(NIP)
                .authorizedNip(OTHER_NIP)
                .invoiceRead();
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testPermissionsRevokeBuilder_toBuilder_isEquivalent() {
        TestPermissionsRevokeBuilder original = TestPermissionsRevokeBuilder.create(NIP)
                .authorizedNip(OTHER_NIP);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testPersonCreateBuilder_toBuilder_isEquivalent() {
        TestPersonCreateBuilder original = TestPersonCreateBuilder.create(NIP, PESEL, false, DESCRIPTION)
                .createdDate(FIXED_DATE);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testRateLimitsBuilder_toBuilder_isEquivalent() {
        TestRateLimitsBuilder original = TestRateLimitsBuilder.create()
                .onlineSession(10, 100, 1000)
                .invoiceSend(5, 50, 500);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testSessionLimitsBuilder_toBuilder_isEquivalent() {
        TestSessionLimitsBuilder original = TestSessionLimitsBuilder.create()
                .onlineSession(10, 20, 50)
                .batchSession(10, 20, 100);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testSubjectCreateBuilder_toBuilder_isEquivalent() {
        TestSubjectCreateBuilder original = TestSubjectCreateBuilder.create(NIP, TestSubjectType.JST, DESCRIPTION)
                .addSubunit(OTHER_NIP, "Subunit description")
                .createdDate(FIXED_DATE);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    @Test
    void testSubjectLimitsBuilder_toBuilder_isEquivalent() {
        TestSubjectLimitsBuilder original = TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                .maxEnrollments(20)
                .maxCertificates(10);
        assertRoundTrip(original.build(), original.toBuilder().build());
    }

    private static <T> void assertRoundTrip(T originalRaw, T copyRaw) {
        assertNotNull(originalRaw);
        assertNotNull(copyRaw);
        assertEquals(originalRaw, copyRaw);
    }
}
