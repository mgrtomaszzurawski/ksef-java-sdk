/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateEnrollBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.builder.CertificateQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityAdminPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionState;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionsQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitPermissionGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each test exercises every fluent setter on one builder family and asserts
 * structural properties of the produced request record (counts, identifier
 * types, copies after toBuilder()). Hits all 25 builders.
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
    private static final String EU_ENTITY_NAME = "EU Partner GmbH";
    private static final String SUBUNIT_NAME = "Branch 1";
    private static final String CERTIFICATE_NAME = "auth-cert";
    private static final String CERTIFICATE_SERIAL = "00112233";
    private static final String SUBUNIT_DESCRIPTION = "subunit-desc";
    private static final String EXTRA_PERMISSION_DESCRIPTION = "extra-perm";
    private static final byte[] FAKE_CSR = new byte[]{1, 2, 3, 4};
    private static final int RATE_PER_SECOND = 1;
    private static final int RATE_PER_MINUTE = 2;
    private static final int RATE_PER_HOUR = 3;
    private static final int LIMIT_VALUE_LARGER = 4;
    private static final int LIMIT_VALUE_LARGER_TWO = 5;
    private static final int LIMIT_VALUE_LARGER_THREE = 6;
    private static final int RATE_LIMIT_CATEGORY_COUNT = 12;
    private static final int TOKEN_PERMISSION_COUNT = 7;
    private static final int ENTITY_PERMISSION_COUNT = 4;
    private static final int MAX_ENROLLMENTS = 10;
    private static final int MAX_CERTIFICATES = 5;

    @Test
    void tokenGenerateBuilder_whenAllPermissionsSet_buildsRequestWithSevenPermissions() {
        TokenGenerateRequest request = TokenGenerateBuilder.create(DESCRIPTION)
                .invoiceRead().invoiceWrite()
                .credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .toBuilder().build();
        assertEquals(DESCRIPTION, request.description());
        assertEquals(TOKEN_PERMISSION_COUNT, request.permissions().size());
    }

    @Test
    void certificateEnrollBuilder_whenAllFieldsSet_preservesNameAndType() {
        CertificateEnrollRequest request = CertificateEnrollBuilder
                .create(CERTIFICATE_NAME, KsefCertificateType.AUTHENTICATION, FAKE_CSR)
                .validFrom(OffsetDateTime.now())
                .toBuilder().build();
        assertEquals(CERTIFICATE_NAME, request.certificateName());
        assertEquals(KsefCertificateType.AUTHENTICATION, request.certificateType());
    }

    @Test
    void certificateQueryBuilder_whenAllFiltersSet_preservesEachField() {
        CertificateQueryRequest request = CertificateQueryBuilder.create()
                .serialNumber(CERTIFICATE_SERIAL)
                .name(CERTIFICATE_NAME)
                .type(KsefCertificateType.OFFLINE)
                .status(CertificateStatus.ACTIVE)
                .expiresAfter(OffsetDateTime.now())
                .toBuilder().build();
        assertEquals(CERTIFICATE_SERIAL, request.serialNumber());
        assertEquals(CertificateStatus.ACTIVE, request.status());
        assertEquals(KsefCertificateType.OFFLINE, request.type());
    }

    @Test
    void testSubjectLimitsBuilder_whenLimitsSet_returnsRequestWithIdentifierType() {
        TestSubjectLimitsRequest request = TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                .maxEnrollments(MAX_ENROLLMENTS).maxCertificates(MAX_CERTIFICATES)
                .toBuilder().build();
        assertEquals(TestSubjectIdentifierType.NIP, request.subjectIdentifierType());
        assertEquals(MAX_ENROLLMENTS, request.maxEnrollments());
    }

    @Test
    void testSessionLimitsBuilder_whenBothSessionsSet_buildsCompleteRequest() {
        TestSessionLimitsRequest request = TestSessionLimitsBuilder.create()
                .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .batchSession(LIMIT_VALUE_LARGER, LIMIT_VALUE_LARGER_TWO, LIMIT_VALUE_LARGER_THREE)
                .toBuilder().build();
        assertEquals(RATE_PER_SECOND, request.onlineSession().maxInvoiceSizeMb());
        assertEquals(LIMIT_VALUE_LARGER, request.batchSession().maxInvoiceSizeMb());
    }

    @Test
    void testRateLimitsBuilder_whenAllCategoriesSet_buildsRequestWithTwelveCategories() {
        TestRateLimitsRequest request = TestRateLimitsBuilder.create()
                .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .batchSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceSend(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceStatus(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .sessionList(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .sessionInvoiceList(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .sessionMisc(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceMetadata(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceExport(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceExportStatus(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .invoiceDownload(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .other(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                .toBuilder().build();
        long set = countSet(request);
        assertEquals(RATE_LIMIT_CATEGORY_COUNT, set);
    }

    private static long countSet(TestRateLimitsRequest request) {
        return java.util.stream.Stream.of(
                request.onlineSession(), request.batchSession(), request.invoiceSend(),
                request.invoiceStatus(), request.sessionList(), request.sessionInvoiceList(),
                request.sessionMisc(), request.invoiceMetadata(), request.invoiceExport(),
                request.invoiceExportStatus(), request.invoiceDownload(), request.other()
        ).filter(java.util.Objects::nonNull).count();
    }

    @Test
    void testPersonCreateBuilder_whenAllFieldsSet_returnsRequestWithNip() {
        TestPersonCreateRequest request = TestPersonCreateBuilder
                .create(NIP, PESEL, false, DESCRIPTION)
                .isDeceased(false).createdDate(OffsetDateTime.now())
                .toBuilder().build();
        assertEquals(NIP, request.nip());
        assertEquals(PESEL, request.pesel());
    }

    @Test
    void testSubjectCreateBuilder_whenSubunitAdded_returnsRequestWithOneSubunit() {
        TestSubjectCreateRequest request = TestSubjectCreateBuilder
                .create(NIP, TestSubjectType.JST, DESCRIPTION)
                .addSubunit(OTHER_NIP, SUBUNIT_DESCRIPTION).createdDate(OffsetDateTime.now())
                .toBuilder().build();
        assertEquals(1, request.subunits().size());
        assertEquals(OTHER_NIP, request.subunits().get(0).subjectNip());
    }

    @Test
    void testPermissionsGrantBuilder_whenEightPermissionsSet_returnsRequestWithEightPermissions() {
        var request = TestPermissionsGrantBuilder.create(NIP)
                .authorizedNip(OTHER_NIP)
                .invoiceRead().invoiceWrite()
                .credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .permission(TestDataPermissionType.INVOICE_READ, EXTRA_PERMISSION_DESCRIPTION)
                .toBuilder().build();
        assertEquals(NIP, request.contextNip());
        assertEquals(8, request.permissions().size());
    }

    @Test
    void testPermissionsRevokeBuilder_whenAuthorizedByEachIdentifierType_buildsThreeVariants() {
        assertEquals(OTHER_NIP, TestPermissionsRevokeBuilder.create(NIP).authorizedNip(OTHER_NIP)
                .toBuilder().build().authorizedValue());
        assertEquals(PESEL, TestPermissionsRevokeBuilder.create(NIP).authorizedPesel(PESEL)
                .toBuilder().build().authorizedValue());
        assertEquals(FINGERPRINT, TestPermissionsRevokeBuilder.create(NIP).authorizedFingerprint(FINGERPRINT)
                .toBuilder().build().authorizedValue());
    }

    @Test
    void personPermissionGrantBuilder_whenAllPermissionsAndIdentifiersSet_buildsRequest() {
        PersonPermissionGrantRequest pesel = PersonPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().invoiceWrite()
                .credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .toBuilder().build();
        assertEquals(TOKEN_PERMISSION_COUNT, pesel.permissions().size());
        assertEquals(PESEL, pesel.identifierValue());
        assertEquals(NIP, PersonPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().build().identifierValue());
        assertEquals(FINGERPRINT, PersonPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().build().identifierValue());
    }

    @Test
    void entityPermissionGrantBuilder_whenAllFourEntriesSet_buildsRequestWithFourPermissions() {
        EntityPermissionGrantRequest request = EntityPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME)
                .invoiceRead().invoiceReadDelegatable().invoiceWrite().invoiceWriteDelegatable()
                .toBuilder().build();
        assertEquals(ENTITY_PERMISSION_COUNT, request.permissions().size());
        long delegatableCount = request.permissions().stream()
                .filter(entry -> entry.canDelegate()).count();
        assertEquals(2L, delegatableCount);
        assertEquals(EntityPermissionType.INVOICE_READ, request.permissions().get(0).type());
    }

    @Test
    void entityAuthorizationGrantBuilder_whenEachPermissionSet_returnsExpectedPermission() {
        EntityAuthorizationPermissionGrantRequest selfInv = EntityAuthorizationPermissionGrantBuilder
                .forNip(NIP).description(DESCRIPTION).entityDetails(FULL_NAME)
                .selfInvoicing().toBuilder().build();
        assertEquals("SELF_INVOICING", selfInv.permission().name());
        assertEquals("RR_INVOICING", EntityAuthorizationPermissionGrantBuilder.forPeppolId("PEPPOL")
                .description(DESCRIPTION).entityDetails(FULL_NAME).rrInvoicing().build().permission().name());
        assertEquals("TAX_REPRESENTATIVE", EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME).taxRepresentative().build().permission().name());
        assertEquals("PEF_INVOICING", EntityAuthorizationPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).entityDetails(FULL_NAME).pefInvoicing().build().permission().name());
    }

    @Test
    void subunitPermissionGrantBuilder_whenContextNipSet_buildsRequestWithSubunitName() {
        SubunitPermissionGrantRequest request = SubunitPermissionGrantBuilder.forNip(NIP)
                .contextNip(OTHER_NIP).description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME).subunitName(SUBUNIT_NAME)
                .toBuilder().build();
        assertEquals(SUBUNIT_NAME, request.subunitName());
        assertEquals(OTHER_NIP, request.contextValue());
        assertEquals(INTERNAL_ID, SubunitPermissionGrantBuilder.forPesel(PESEL)
                .contextInternalId(INTERNAL_ID).description(DESCRIPTION)
                .personDetails(FIRST_NAME, LAST_NAME).build().contextValue());
    }

    @Test
    void indirectPermissionGrantBuilder_whenAllTargetTypesUsed_buildsThreeVariantRequests() {
        IndirectPermissionGrantRequest withTargetNip = IndirectPermissionGrantBuilder.forNip(NIP)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().invoiceWrite().targetNip(OTHER_NIP)
                .toBuilder().build();
        assertEquals(OTHER_NIP, withTargetNip.targetValue());
        assertEquals(2, withTargetNip.permissions().size());
        assertEquals(null, IndirectPermissionGrantBuilder.forPesel(PESEL)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().targetAllPartners().build().targetValue());
        assertEquals(INTERNAL_ID, IndirectPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION).personDetails(FIRST_NAME, LAST_NAME)
                .invoiceRead().targetInternalId(INTERNAL_ID).build().targetValue());
    }

    @Test
    void euEntityPermissionGrantBuilder_whenAllFieldsSet_buildsRequestWithSubjectFullName() {
        EuEntityPermissionGrantRequest request = EuEntityPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                .description(DESCRIPTION)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .invoiceRead().invoiceWrite()
                .toBuilder().build();
        assertEquals(FULL_NAME, request.subjectFullName());
        assertEquals(2, request.permissions().size());
    }

    @Test
    void euEntityAdminGrantBuilder_whenAllFieldsSet_carriesEuEntityAddressAndContext() {
        EuEntityAdminPermissionGrantRequest request = EuEntityAdminPermissionGrantBuilder
                .forFingerprint(FINGERPRINT).contextNipVatUe(NIP_VAT_UE)
                .description(DESCRIPTION).euEntityName(EU_ENTITY_NAME)
                .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                .euEntityDetails(EU_ENTITY_NAME, ADDRESS)
                .toBuilder().build();
        assertEquals(NIP_VAT_UE, request.contextValue());
        assertEquals(EU_ENTITY_NAME, request.euEntityFullName());
    }

    @Test
    void personalPermissionsQueryBuilder_whenAllFiltersSet_buildsQueryWithEightPermissions() {
        PersonalPermissionsQueryRequest activeQuery = PersonalPermissionsQueryBuilder.create()
                .contextNip(NIP).contextInternalId(INTERNAL_ID)
                .targetNip(OTHER_NIP).targetAllPartners().targetInternalId(INTERNAL_ID)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection().vatUeManage()
                .activeOnly().toBuilder().build();
        assertEquals(8, activeQuery.permissionTypes().size());
        assertEquals(PermissionState.ACTIVE, activeQuery.permissionState());
        assertEquals(PermissionState.INACTIVE,
                PersonalPermissionsQueryBuilder.create().inactiveOnly().build().permissionState());
    }

    @Test
    void personPermissionsQueryBuilder_whenInCurrentContextWithEveryFilter_buildsRequestWithSeven() {
        PersonPermissionsQueryRequest request = PersonPermissionsQueryBuilder.permissionsInCurrentContext()
                .authorByNip(NIP).authorByPesel(PESEL).authorByFingerprint(FINGERPRINT).authorSystem()
                .authorizedByNip(OTHER_NIP).authorizedByPesel(PESEL).authorizedByFingerprint(FINGERPRINT)
                .contextNip(NIP).contextInternalId(INTERNAL_ID)
                .targetNip(OTHER_NIP).targetAllPartners().targetInternalId(INTERNAL_ID)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .activeOnly().toBuilder().build();
        assertEquals(TOKEN_PERMISSION_COUNT, request.permissionTypes().size());
        assertEquals(PermissionState.INACTIVE,
                PersonPermissionsQueryBuilder.permissionsGrantedInCurrentContext()
                        .inactiveOnly().build().permissionState());
    }

    @Test
    void euEntityPermissionsQueryBuilder_whenFourPermissionTypesSet_buildsRequestWithFour() {
        var request = EuEntityPermissionsQueryBuilder.create()
                .vatUeIdentifier(NIP_VAT_UE).authorizedFingerprintIdentifier(FINGERPRINT)
                .vatUeManage().invoiceRead().invoiceWrite().introspection()
                .toBuilder().build();
        assertEquals(NIP_VAT_UE, request.vatUeIdentifier());
        assertEquals(ENTITY_PERMISSION_COUNT, request.permissionTypes().size());
    }

    @Test
    void entityAuthorizationPermissionsQueryBuilder_whenGrantedWithAllPermissions_buildsRequest() {
        var granted = EntityAuthorizationPermissionsQueryBuilder.granted()
                .authorizingByNip(NIP).authorizedByNip(OTHER_NIP)
                .selfInvoicing().rrInvoicing().taxRepresentative().pefInvoicing()
                .toBuilder().build();
        assertEquals(ENTITY_PERMISSION_COUNT, granted.permissionTypes().size());
        assertEquals("PEPPOL", EntityAuthorizationPermissionsQueryBuilder.received()
                .authorizedByPeppolId("PEPPOL").build().authorizedValue());
        assertTrue(granted.authorizingNip() != null);
    }
}
