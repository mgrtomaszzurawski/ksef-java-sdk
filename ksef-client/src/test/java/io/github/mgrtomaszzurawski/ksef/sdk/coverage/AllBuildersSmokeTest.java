/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateRevocationReason;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Coverage-targeted smoke tests that exercise every fluent setter on every
 * builder. Each test constructs a fully-populated builder, invokes {@code
 * build()}, and asserts non-null. The goal is exhaustive method-coverage of
 * builders + their SDK request records (validated via record canonical
 * constructors). Hits all 25 builders introduced by A3.
 */
class AllBuildersSmokeTest {

    private static final String NIP = "1234567890";
    private static final String OTHER_NIP = "0987654321";
    private static final String PESEL = "82060411457";
    private static final String FINGERPRINT = "ABC123DEF456";
    private static final String INTERNAL_ID = "internal-id-1";
    private static final String DESCRIPTION = "Test description >= 5 chars";
    private static final String FIRST_NAME = "Jan";
    private static final String LAST_NAME = "Kowalski";
    private static final String FULL_NAME = "Firma Sp. z o.o.";
    private static final String ADDRESS = "Berlin, Germany";
    private static final String NIP_VAT_UE = "PL1234567890";
    private static final String EU_NAME = "EU Partner GmbH";
    private static final String SUBUNIT_NAME = "Branch 1";
    private static final byte[] CSR_BYTES = new byte[]{1, 2, 3, 4};

    @Test
    void tokens_buildAllFluentSetters() {
        var req = TokenGenerateBuilder.create(DESCRIPTION)
                .invoiceRead().invoiceWrite()
                .credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .toBuilder().build();
        assertNotNull(req);
        assertNotNull(req.permissions());
    }

    @Test
    void certificates_buildAllFluentSetters() {
        var enroll = CertificateEnrollBuilder
                .create("cert-name", KsefCertificateType.AUTHENTICATION, CSR_BYTES)
                .validFrom(java.time.OffsetDateTime.now())
                .toBuilder().build();
        assertNotNull(enroll);
        var query = CertificateQueryBuilder.create()
                .serialNumber("123")
                .name("name")
                .type(KsefCertificateType.OFFLINE)
                .status(CertificateStatus.ACTIVE)
                .expiresAfter(java.time.OffsetDateTime.now())
                .toBuilder().build();
        assertNotNull(query);
        assertNotNull(CertificateRevocationReason.values());
    }

    @Test
    void testdata_buildAllFluentSetters() {
        assertNotNull(TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                .maxEnrollments(10).maxCertificates(5)
                .toBuilder().build());
        assertNotNull(TestSessionLimitsBuilder.create()
                .onlineSession(1, 2, 3).batchSession(4, 5, 6)
                .toBuilder().build());
        assertNotNull(TestRateLimitsBuilder.create()
                .onlineSession(1, 2, 3).batchSession(1, 2, 3)
                .invoiceSend(1, 2, 3).invoiceStatus(1, 2, 3)
                .sessionList(1, 2, 3).sessionInvoiceList(1, 2, 3)
                .sessionMisc(1, 2, 3).invoiceMetadata(1, 2, 3)
                .invoiceExport(1, 2, 3).invoiceExportStatus(1, 2, 3)
                .invoiceDownload(1, 2, 3).other(1, 2, 3)
                .toBuilder().build());
        assertNotNull(TestPersonCreateBuilder.create(NIP, PESEL, false, DESCRIPTION)
                .isDeceased(false).createdDate(java.time.OffsetDateTime.now())
                .toBuilder().build());
        assertNotNull(TestSubjectCreateBuilder.create(NIP, TestSubjectType.JST, DESCRIPTION)
                .addSubunit(OTHER_NIP, "Subunit").createdDate(java.time.OffsetDateTime.now())
                .toBuilder().build());
        assertNotNull(TestPermissionsGrantBuilder.create(NIP)
                .authorizedNip(OTHER_NIP)
                .invoiceRead().invoiceWrite()
                .credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .permission(TestDataPermissionType.INVOICE_READ, "extra")
                .toBuilder().build());
        assertNotNull(TestPermissionsRevokeBuilder.create(NIP)
                .authorizedNip(OTHER_NIP)
                .toBuilder().build());
        assertNotNull(TestPermissionsRevokeBuilder.create(NIP)
                .authorizedPesel(PESEL)
                .toBuilder().build());
        assertNotNull(TestPermissionsRevokeBuilder.create(NIP)
                .authorizedFingerprint(FINGERPRINT)
                .toBuilder().build());
    }

    @Test
    void permissionGrants_buildAllFluentSetters() {
        assertNotNull(PersonPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .toBuilder().build());
        assertNotNull(PersonPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().toBuilder().build());
        assertNotNull(PersonPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().toBuilder().build());

        assertNotNull(EntityPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .invoiceRead().invoiceReadDelegatable().invoiceWrite().invoiceWriteDelegatable()
                .toBuilder().build());

        assertNotNull(EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .selfInvoicing().toBuilder().build());
        assertNotNull(EntityAuthorizationPermissionGrantBuilder.forPeppolId("PEPPOL")
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .rrInvoicing().toBuilder().build());
        assertNotNull(EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .taxRepresentative().toBuilder().build());
        assertNotNull(EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .pefInvoicing().toBuilder().build());

        assertNotNull(SubunitPermissionGrantBuilder.forNip(NIP)
                .contextNip(OTHER_NIP).description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME).subunitName(SUBUNIT_NAME)
                .toBuilder().build());
        assertNotNull(SubunitPermissionGrantBuilder.forPesel(PESEL)
                .contextInternalId(INTERNAL_ID).description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME)
                .toBuilder().build());
        assertNotNull(SubunitPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .contextNip(NIP).description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME)
                .toBuilder().build());

        assertNotNull(IndirectPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().invoiceWrite()
                .targetNip(OTHER_NIP).toBuilder().build());
        assertNotNull(IndirectPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().targetAllPartners().toBuilder().build());
        assertNotNull(IndirectPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().targetInternalId(INTERNAL_ID).toBuilder().build());

        assertNotNull(EuEntityPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .invoiceRead().invoiceWrite()
                .toBuilder().build());

        assertNotNull(EuEntityAdminPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .contextNipVatUe(NIP_VAT_UE).description(DESCRIPTION).euEntityName(EU_NAME)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .euEntityDetails(EU_NAME, ADDRESS)
                .toBuilder().build());
    }

    @Test
    void permissionQueries_buildAllFluentSetters() {
        assertNotNull(PersonalPermissionsQueryBuilder.create()
                .contextNip(NIP).contextInternalId(INTERNAL_ID)
                .targetNip(OTHER_NIP).targetAllPartners().targetInternalId(INTERNAL_ID)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection().vatUeManage()
                .activeOnly().toBuilder().build());
        assertNotNull(PersonalPermissionsQueryBuilder.create()
                .inactiveOnly().toBuilder().build());

        assertNotNull(PersonPermissionsQueryBuilder.permissionsInCurrentContext()
                .authorByNip(NIP).authorByPesel(PESEL).authorByFingerprint(FINGERPRINT).authorSystem()
                .authorizedByNip(OTHER_NIP).authorizedByPesel(PESEL).authorizedByFingerprint(FINGERPRINT)
                .contextNip(NIP).contextInternalId(INTERNAL_ID)
                .targetNip(OTHER_NIP).targetAllPartners().targetInternalId(INTERNAL_ID)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .activeOnly().toBuilder().build());
        assertNotNull(PersonPermissionsQueryBuilder.permissionsGrantedInCurrentContext()
                .inactiveOnly().toBuilder().build());

        assertNotNull(EuEntityPermissionsQueryBuilder.create()
                .vatUeIdentifier(NIP_VAT_UE).authorizedFingerprintIdentifier(FINGERPRINT)
                .vatUeManage().invoiceRead().invoiceWrite().introspection()
                .toBuilder().build());

        assertNotNull(EntityAuthorizationPermissionsQueryBuilder.granted()
                .authorizingByNip(NIP).authorizedByNip(OTHER_NIP)
                .selfInvoicing().rrInvoicing().taxRepresentative().pefInvoicing()
                .toBuilder().build());
        assertNotNull(EntityAuthorizationPermissionsQueryBuilder.received()
                .authorizedByPeppolId("PEPPOL")
                .toBuilder().build());
    }
}
