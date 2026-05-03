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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.SendInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
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
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.AuthorizationQueryType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityAuthorizationPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EntityPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.EuEntityQueryPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.IndirectTargetIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionState;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonAuthorIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonPermissionsQueryType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalContextIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonalTargetIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.SubunitContextIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.builder.TokenGenerateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.certificates.mapping.CertificatesMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping.InvoicingRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionEnumConverters;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionsQueryRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.permissions.mapping.PermissionsRequestMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping.TestdataMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.tokens.mapping.TokensMappers;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each test exercises one mapper class's public methods via builder→Raw
 * round trips. Assertions check structural properties of the produced Raw
 * instance (description, NIP, permissions count) rather than just non-null,
 * so a mapper drop-field regression would surface here.
 */
class AllMappersSmokeTest {

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
    private static final String CERTIFICATE_NAME = "auth-cert";
    private static final int AES_256_KEY_SIZE_BYTES = 32;
    private static final int AES_CBC_IV_SIZE_BYTES = 16;
    private static final byte[] FAKE_CSR = new byte[]{1};
    private static final byte[] FAKE_AES_KEY = new byte[AES_256_KEY_SIZE_BYTES];
    private static final byte[] FAKE_INIT_VECTOR = new byte[AES_CBC_IV_SIZE_BYTES];
    private static final byte[] FAKE_INVOICE_BYTES = new byte[]{1};
    private static final int RATE_PER_SECOND = 1;
    private static final int RATE_PER_MINUTE = 2;
    private static final int RATE_PER_HOUR = 3;
    private static final int MAX_ENROLLMENTS = 1;
    private static final int MAX_CERTIFICATES = 2;
    private static final String QUERY_SERIAL = "test-serial";
    private static final String QUERY_NAME = "test-name";
    private static final String QUERY_KSEF_NUMBER = "test-ksef-1";
    private static final String QUERY_INVOICE_NUMBER = "test-invoice-1";
    private static final String SUBUNIT_DESC = "subunit-desc";
    private static final String PEPPOL_ID_FIXTURE = "PL:0007:1234567890";
    private static final int TOKEN_PERMISSION_COUNT_FOR_SDK = 7;
    private static final int ENTITY_AUTHORIZATION_PERMISSION_COUNT = 4;

    @Test
    void tokensMappers_toGenerateTokenRequestRaw_preservesDescriptionAndPermissionCount() {
        var sdkRequest = TokenGenerateBuilder.create(DESCRIPTION)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .build();
        var rawRequest = TokensMappers.toGenerateTokenRequestRaw(sdkRequest);
        assertEquals(DESCRIPTION, rawRequest.getDescription());
        assertEquals(TOKEN_PERMISSION_COUNT_FOR_SDK, rawRequest.getPermissions().size());
        for (TokenPermissionType type : TokenPermissionType.values()) {
            assertNotNull(TokensMappers.toTokenPermissionTypeRaw(type));
        }
    }

    @Test
    void certificatesMappers_toEnrollAndQueryRequestRaw_preserveFields() {
        var enrollSdk = CertificateEnrollBuilder
                .create(CERTIFICATE_NAME, KsefCertificateType.AUTHENTICATION, FAKE_CSR)
                .validFrom(OffsetDateTime.now()).build();
        var enrollRaw = CertificatesMappers.toEnrollCertificateRequestRaw(enrollSdk);
        assertEquals(CERTIFICATE_NAME, enrollRaw.getCertificateName());
        var querySdk = CertificateQueryBuilder.create()
                .serialNumber(QUERY_SERIAL).name(QUERY_NAME)
                .type(KsefCertificateType.OFFLINE).status(CertificateStatus.ACTIVE)
                .expiresAfter(OffsetDateTime.now()).build();
        var queryRaw = CertificatesMappers.toQueryCertificatesRequestRaw(querySdk);
        assertEquals(QUERY_SERIAL, queryRaw.getCertificateSerialNumber());
        for (KsefCertificateType type : KsefCertificateType.values()) {
            assertEquals(type.name(), CertificatesMappers.toKsefCertificateTypeRaw(type).name());
        }
        for (CertificateStatus status : CertificateStatus.values()) {
            assertEquals(status.name(), CertificatesMappers.toCertificateListItemStatusRaw(status).name());
        }
        for (CertificateRevocationReason reason : CertificateRevocationReason.values()) {
            assertEquals(reason.name(), CertificatesMappers.toCertificateRevocationReasonRaw(reason).name());
        }
    }

    @Test
    void invoicingRequestMappers_toInvoiceQueryFiltersRaw_handlesAllSubjectTypes() {
        var sellerQuery = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(OffsetDateTime.now())
                .dateTo(OffsetDateTime.now().plusDays(1))
                .ksefNumber(QUERY_KSEF_NUMBER).invoiceNumber(QUERY_INVOICE_NUMBER).sellerNip(NIP)
                .onlineOnly().selfInvoicing(true).hasAttachment(true).build();
        var sellerRaw = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(sellerQuery);
        assertEquals(NIP, sellerRaw.getSellerNip());
        assertNotNull(InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.buyer().permanentStorageDateFrom(OffsetDateTime.now()).offlineOnly().build()));
        assertNotNull(InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.thirdParty().issueDateFrom(OffsetDateTime.now()).build()));
        assertNotNull(InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.authorized().invoicingDateFrom(OffsetDateTime.now()).build()));
        assertEquals(InvoicingMode.ONLINE.name(),
                InvoicingRequestMappers.toInvoicingModeRaw(InvoicingMode.ONLINE).name());
        assertEquals(InvoicingMode.OFFLINE.name(),
                InvoicingRequestMappers.toInvoicingModeRaw(InvoicingMode.OFFLINE).name());
    }

    @Test
    void invoicingRequestMappers_toSendInvoiceRequestRaw_preservesOfflineFlagAndSizes() {
        var sendSdk = SendInvoiceBuilder.create(FAKE_INVOICE_BYTES, FAKE_AES_KEY, FAKE_INIT_VECTOR)
                .offline().build();
        var sendRaw = InvoicingRequestMappers.toSendInvoiceRequestRaw(sendSdk);
        assertTrue(sendRaw.getOfflineMode());
        assertEquals((long) FAKE_INVOICE_BYTES.length, sendRaw.getInvoiceSize());
    }

    @Test
    void testdataMappers_toRawForEveryBuilder_succeeds() {
        assertNotNull(TestdataMappers.toSetSubjectLimitsRequestRaw(
                TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                        .maxEnrollments(MAX_ENROLLMENTS).maxCertificates(MAX_CERTIFICATES).build()));
        assertNotNull(TestdataMappers.toSetSessionLimitsRequestRaw(
                TestSessionLimitsBuilder.create()
                        .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                        .batchSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR).build()));
        assertNotNull(TestdataMappers.toSetRateLimitsRequestRaw(
                TestRateLimitsBuilder.create()
                        .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR).build()));
        assertNotNull(TestdataMappers.toPersonCreateRequestRaw(
                TestPersonCreateBuilder.create(NIP, PESEL, true, DESCRIPTION)
                        .isDeceased(true).createdDate(OffsetDateTime.now()).build()));
        assertNotNull(TestdataMappers.toSubjectCreateRequestRaw(
                TestSubjectCreateBuilder.create(NIP, TestSubjectType.JST, DESCRIPTION)
                        .addSubunit(OTHER_NIP, SUBUNIT_DESC).createdDate(OffsetDateTime.now()).build()));
        var grantRaw = TestdataMappers.toTestDataPermissionsGrantRequestRaw(
                TestPermissionsGrantBuilder.create(NIP)
                        .authorizedNip(OTHER_NIP).invoiceRead().invoiceWrite()
                        .credentialsRead().credentialsManage().subunitManage()
                        .enforcementOperations().introspection().build());
        assertEquals(TOKEN_PERMISSION_COUNT_FOR_SDK, grantRaw.getPermissions().size());
        assertNotNull(TestdataMappers.toTestDataPermissionsRevokeRequestRaw(
                TestPermissionsRevokeBuilder.create(NIP).authorizedPesel(PESEL).build()));
        for (TestSubjectIdentifierType type : TestSubjectIdentifierType.values()) {
            assertEquals(type.name(), TestdataMappers.toSubjectIdentifierTypeRaw(type).name());
        }
        for (TestSubjectType type : TestSubjectType.values()) {
            assertEquals(type.name(), TestdataMappers.toSubjectTypeRaw(type).name());
        }
        for (TestDataIdentifierType type : TestDataIdentifierType.values()) {
            assertEquals(type.name(),
                    TestdataMappers.toTestDataAuthenticationContextIdentifierTypeRaw(type).name());
        }
        for (TestDataPermissionType type : TestDataPermissionType.values()) {
            assertEquals(type.name(), TestdataMappers.toTestDataPermissionTypeRaw(type).name());
        }
    }

    @Test
    void permissionsRequestMappers_toGrantRequestRawForEachBuilder_succeeds() {
        assertNotNull(PermissionsRequestMappers.toPersonPermissionsGrantRequestRaw(
                PersonPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).invoiceRead().build()));
        var entityRaw = PermissionsRequestMappers.toEntityPermissionsGrantRequestRaw(
                EntityPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION).entityDetails(FULL_NAME)
                        .invoiceRead().invoiceReadDelegatable().invoiceWrite().invoiceWriteDelegatable().build());
        assertEquals(ENTITY_AUTHORIZATION_PERMISSION_COUNT, entityRaw.getPermissions().size());
        assertNotNull(PermissionsRequestMappers.toEntityAuthorizationPermissionsGrantRequestRaw(
                EntityAuthorizationPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .entityDetails(FULL_NAME).selfInvoicing().build()));
        assertNotNull(PermissionsRequestMappers.toIndirectPermissionsGrantRequestRaw(
                IndirectPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).invoiceRead().targetNip(OTHER_NIP).build()));
        assertNotNull(PermissionsRequestMappers.toSubunitPermissionsGrantRequestRaw(
                SubunitPermissionGrantBuilder.forNip(NIP).contextNip(OTHER_NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).build()));
        assertNotNull(PermissionsRequestMappers.toEuEntityPermissionsGrantRequestRaw(
                EuEntityPermissionGrantBuilder.forFingerprint(FINGERPRINT).description(DESCRIPTION)
                        .subjectEntityByFingerprint(FULL_NAME, ADDRESS).invoiceRead().build()));
        assertNotNull(PermissionsRequestMappers.toEuEntityAdministrationPermissionsGrantRequestRaw(
                EuEntityAdminPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                        .contextNipVatUe(NIP_VAT_UE).description(DESCRIPTION).euEntityName(EU_ENTITY_NAME)
                        .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                        .euEntityDetails(EU_ENTITY_NAME, ADDRESS).build()));
    }

    @Test
    void permissionsRequestMappers_enumConvertersExposeRawWithSameName() {
        for (PersonSubjectIdentifierType type : PersonSubjectIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionsRequestMappers.toPersonSubjectIdentifierTypeRaw(type).name());
            assertEquals(type.name(),
                    PermissionsRequestMappers.toSubunitSubjectIdentifierTypeRaw(type).name());
            assertEquals(type.name(),
                    PermissionsRequestMappers.toIndirectSubjectIdentifierTypeRaw(type).name());
        }
        for (PersonPermissionType type : PersonPermissionType.values()) {
            assertEquals(type.name(), PermissionsRequestMappers.toPersonPermissionTypeRaw(type).name());
        }
        for (EntityPermissionType type : EntityPermissionType.values()) {
            assertEquals(type.name(), PermissionsRequestMappers.toEntityPermissionTypeRaw(type).name());
        }
        for (EuEntityPermissionType type : EuEntityPermissionType.values()) {
            assertEquals(type.name(), PermissionsRequestMappers.toEuEntityPermissionTypeRaw(type).name());
        }
        for (SubunitContextIdentifierType type : SubunitContextIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionsRequestMappers.toSubunitContextIdentifierTypeRaw(type).name());
        }
        for (IndirectTargetIdentifierType type : IndirectTargetIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionsRequestMappers.toIndirectTargetIdentifierTypeRaw(type).name());
        }
        for (IndirectPermissionType type : IndirectPermissionType.values()) {
            assertEquals(type.name(), PermissionsRequestMappers.toIndirectPermissionTypeRaw(type).name());
        }
    }

    @Test
    void permissionsQueryRequestMappers_toQueryRequestRawForEachBuilder_succeeds() {
        var authQuery = PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(
                EntityAuthorizationPermissionsQueryBuilder.granted()
                        .authorizingByNip(NIP).authorizedByNip(OTHER_NIP).selfInvoicing().build());
        assertEquals(NIP, authQuery.getAuthorizingIdentifier().getValue());
        assertNotNull(PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(
                EntityAuthorizationPermissionsQueryBuilder.received().authorizedByPeppolId(PEPPOL_ID_FIXTURE).build()));
        var personQuery = PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(
                PersonPermissionsQueryBuilder.permissionsInCurrentContext()
                        .authorByNip(NIP).authorizedByPesel(PESEL).contextNip(NIP).targetNip(OTHER_NIP)
                        .invoiceRead().activeOnly().build());
        assertEquals(NIP, personQuery.getAuthorIdentifier().getValue());
        assertNotNull(PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(
                PersonPermissionsQueryBuilder.permissionsGrantedInCurrentContext().authorSystem().build()));
        var personalQuery = PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create()
                        .contextNip(NIP).targetNip(OTHER_NIP).invoiceRead().vatUeManage().activeOnly().build());
        assertEquals(NIP, personalQuery.getContextIdentifier().getValue());
        assertNotNull(PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create().contextInternalId(INTERNAL_ID)
                        .targetAllPartners().inactiveOnly().build()));
        assertNotNull(PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create().targetInternalId(INTERNAL_ID).build()));
        var euQuery = PermissionsQueryRequestMappers.toEuEntityPermissionsQueryRequestRaw(
                EuEntityPermissionsQueryBuilder.create()
                        .vatUeIdentifier(NIP_VAT_UE).authorizedFingerprintIdentifier(FINGERPRINT)
                        .vatUeManage().invoiceRead().invoiceWrite().introspection().build());
        assertEquals(NIP_VAT_UE, euQuery.getVatUeIdentifier());
    }

    @Test
    void permissionEnumConverters_eachSdkEnumMapsToSameNameRaw() {
        for (AuthorizationQueryType type : AuthorizationQueryType.values()) {
            assertEquals(type.name(), PermissionEnumConverters.toAuthorizationQueryTypeRaw(type).name());
        }
        for (EntityAuthorizationIdentifierType type : EntityAuthorizationIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toAuthorizedEntityIdentifierTypeRaw(type).name());
        }
        for (EntityAuthorizationPermissionType type : EntityAuthorizationPermissionType.values()) {
            assertEquals(type.name(), PermissionEnumConverters.toInvoicePermissionTypeRaw(type).name());
        }
        for (PersonPermissionsQueryType type : PersonPermissionsQueryType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonPermissionsQueryTypeRaw(type).name());
        }
        for (PersonAuthorIdentifierType type : PersonAuthorIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonAuthorIdentifierTypeRaw(type).name());
        }
        for (PersonSubjectIdentifierType type : PersonSubjectIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonAuthorizedIdentifierTypeRaw(type).name());
        }
        for (PersonalContextIdentifierType type : PersonalContextIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonalContextIdentifierTypeRaw(type).name());
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonPermissionsContextIdentifierTypeRaw(type).name());
        }
        for (PersonalTargetIdentifierType type : PersonalTargetIdentifierType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonalTargetIdentifierTypeRaw(type).name());
            assertEquals(type.name(),
                    PermissionEnumConverters.toPersonPermissionsTargetIdentifierTypeRaw(type).name());
        }
        for (PersonalPermissionType type : PersonalPermissionType.values()) {
            assertEquals(type.name(), PermissionEnumConverters.toPersonalPermissionTypeRaw(type).name());
        }
        for (PermissionState state : PermissionState.values()) {
            assertEquals(state.name(), PermissionEnumConverters.toPermissionStateRaw(state).name());
        }
        for (EuEntityQueryPermissionType type : EuEntityQueryPermissionType.values()) {
            assertEquals(type.name(),
                    PermissionEnumConverters.toEuEntityQueryPermissionTypeRaw(type).name());
        }
    }
}
