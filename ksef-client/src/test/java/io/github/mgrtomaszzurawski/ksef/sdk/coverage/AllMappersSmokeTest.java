/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
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
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceQuerySubjectTypeRaw;
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
    private static final int MAX_ENROLLMENTS = 10;
    private static final int MAX_CERTIFICATES = 5;
    private static final String QUERY_SERIAL = "test-serial";
    private static final String QUERY_NAME = "test-name";
    private static final String QUERY_KSEF_NUMBER = "test-ksef-1";
    private static final String QUERY_INVOICE_NUMBER = "test-invoice-1";
    private static final String SUBUNIT_DESC = "subunit-desc";
    private static final String PEPPOL_ID_FIXTURE = "PL:0007:1234567890";
    private static final int TOKEN_PERMISSION_COUNT = 7;
    private static final int ENTITY_AUTHORIZATION_PERMISSION_COUNT = 4;
    private static final int SINGLE_SUBUNIT_COUNT = 1;

    @Test
    void tokensMappers_toGenerateTokenRequestRaw_preservesDescriptionAndPermissionCount() {
        var sdkRequest = TokenGenerateBuilder.create(DESCRIPTION)
                .invoiceRead().invoiceWrite().credentialsRead().credentialsManage()
                .subunitManage().enforcementOperations().introspection()
                .build();
        var rawRequest = TokensMappers.toGenerateTokenRequestRaw(sdkRequest);
        assertEquals(DESCRIPTION, rawRequest.getDescription());
        assertEquals(TOKEN_PERMISSION_COUNT, rawRequest.getPermissions().size());
    }

    @Test
    void tokensMappers_toTokenPermissionTypeRaw_eachSdkEnumMapsToSameNameRaw() {
        for (TokenPermissionType type : TokenPermissionType.values()) {
            assertEquals(type.name(), TokensMappers.toTokenPermissionTypeRaw(type).name());
        }
    }

    @Test
    void certificatesMappers_toEnrollCertificateRequestRaw_preservesCertificateName() {
        var enrollSdk = CertificateEnrollBuilder
                .create(CERTIFICATE_NAME, KsefCertificateType.AUTHENTICATION, FAKE_CSR)
                .validFrom(OffsetDateTime.now()).build();
        var enrollRaw = CertificatesMappers.toEnrollCertificateRequestRaw(enrollSdk);
        assertEquals(CERTIFICATE_NAME, enrollRaw.getCertificateName());
    }

    @Test
    void certificatesMappers_toQueryCertificatesRequestRaw_preservesSerialNumber() {
        var querySdk = CertificateQueryBuilder.create()
                .serialNumber(QUERY_SERIAL).name(QUERY_NAME)
                .type(KsefCertificateType.OFFLINE).status(CertificateStatus.ACTIVE)
                .expiresAfter(OffsetDateTime.now()).build();
        var queryRaw = CertificatesMappers.toQueryCertificatesRequestRaw(querySdk);
        assertEquals(QUERY_SERIAL, queryRaw.getCertificateSerialNumber());
    }

    @Test
    void certificatesMappers_enumConvertersExposeRawWithSameName() {
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
    void invoicingRequestMappers_toInvoiceQueryFiltersRaw_whenSeller_preservesSellerNip() {
        var sellerQuery = InvoiceQueryBuilder.seller()
                .invoicingDateFrom(OffsetDateTime.now())
                .dateTo(OffsetDateTime.now().plusDays(1))
                .ksefNumber(QUERY_KSEF_NUMBER).invoiceNumber(QUERY_INVOICE_NUMBER).sellerNip(NIP)
                .onlineOnly().selfInvoicing(true).hasAttachment(true).build();
        var sellerRaw = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(sellerQuery);
        assertEquals(NIP, sellerRaw.getSellerNip());
        assertEquals(InvoiceQuerySubjectTypeRaw.SUBJECT1, sellerRaw.getSubjectType());
    }

    @Test
    void invoicingRequestMappers_toInvoiceQueryFiltersRaw_whenBuyer_setsSubjectType2() {
        var buyerRaw = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.buyer().permanentStorageDateFrom(OffsetDateTime.now()).offlineOnly().build());
        assertEquals(InvoiceQuerySubjectTypeRaw.SUBJECT2, buyerRaw.getSubjectType());
    }

    @Test
    void invoicingRequestMappers_toInvoiceQueryFiltersRaw_whenThirdParty_setsSubjectType3() {
        var thirdPartyRaw = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.thirdParty().issueDateFrom(OffsetDateTime.now()).build());
        assertEquals(InvoiceQuerySubjectTypeRaw.SUBJECT3, thirdPartyRaw.getSubjectType());
    }

    @Test
    void invoicingRequestMappers_toInvoiceQueryFiltersRaw_whenAuthorized_setsSubjectAuthorized() {
        var authorizedRaw = InvoicingRequestMappers.toInvoiceQueryFiltersRaw(
                InvoiceQueryBuilder.authorized().invoicingDateFrom(OffsetDateTime.now()).build());
        assertEquals(InvoiceQuerySubjectTypeRaw.SUBJECT_AUTHORIZED, authorizedRaw.getSubjectType());
    }

    @Test
    void invoicingRequestMappers_toInvoicingModeRaw_eachSdkEnumMapsToSameNameRaw() {
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
    void testdataMappers_toSetSubjectLimitsRequestRaw_preservesEnrollmentAndCertificateLimits() {
        var rawRequest = TestdataMappers.toSetSubjectLimitsRequestRaw(
                TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                        .maxEnrollments(MAX_ENROLLMENTS).maxCertificates(MAX_CERTIFICATES).build());
        assertEquals(MAX_ENROLLMENTS, rawRequest.getEnrollment().getMaxEnrollments());
        assertEquals(MAX_CERTIFICATES, rawRequest.getCertificate().getMaxCertificates());
    }

    @Test
    void testdataMappers_toSetSessionLimitsRequestRaw_preservesBothSessionTypes() {
        var rawRequest = TestdataMappers.toSetSessionLimitsRequestRaw(
                TestSessionLimitsBuilder.create()
                        .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                        .batchSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR).build());
        assertNotNull(rawRequest.getOnlineSession());
        assertNotNull(rawRequest.getBatchSession());
    }

    @Test
    void testdataMappers_toSetRateLimitsRequestRaw_preservesOnlineSessionRates() {
        var rawRequest = TestdataMappers.toSetRateLimitsRequestRaw(
                TestRateLimitsBuilder.create()
                        .onlineSession(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR).build());
        assertNotNull(rawRequest.getRateLimits().getOnlineSession());
    }

    @Test
    void testdataMappers_toPersonCreateRequestRaw_preservesNipAndPesel() {
        var rawRequest = TestdataMappers.toPersonCreateRequestRaw(
                TestPersonCreateBuilder.create(NIP, PESEL, true, DESCRIPTION)
                        .isDeceased(true).createdDate(OffsetDateTime.now()).build());
        assertEquals(NIP, rawRequest.getNip());
        assertEquals(PESEL, rawRequest.getPesel());
        assertEquals(DESCRIPTION, rawRequest.getDescription());
    }

    @Test
    void testdataMappers_toSubjectCreateRequestRaw_preservesNipAndSubunits() {
        var rawRequest = TestdataMappers.toSubjectCreateRequestRaw(
                TestSubjectCreateBuilder.create(NIP, TestSubjectType.JST, DESCRIPTION)
                        .addSubunit(OTHER_NIP, SUBUNIT_DESC).createdDate(OffsetDateTime.now()).build());
        assertEquals(NIP, rawRequest.getSubjectNip());
        assertEquals(SINGLE_SUBUNIT_COUNT, rawRequest.getSubunits().size());
    }

    @Test
    void testdataMappers_toTestDataPermissionsGrantRequestRaw_preservesPermissionCount() {
        var grantRaw = TestdataMappers.toTestDataPermissionsGrantRequestRaw(
                TestPermissionsGrantBuilder.create(NIP)
                        .authorizedNip(OTHER_NIP).invoiceRead().invoiceWrite()
                        .credentialsRead().credentialsManage().subunitManage()
                        .enforcementOperations().introspection().build());
        assertEquals(TOKEN_PERMISSION_COUNT, grantRaw.getPermissions().size());
        assertEquals(NIP, grantRaw.getContextIdentifier().getValue());
    }

    @Test
    void testdataMappers_toTestDataPermissionsRevokeRequestRaw_preservesAuthorizedPesel() {
        var rawRequest = TestdataMappers.toTestDataPermissionsRevokeRequestRaw(
                TestPermissionsRevokeBuilder.create(NIP).authorizedPesel(PESEL).build());
        assertEquals(PESEL, rawRequest.getAuthorizedIdentifier().getValue());
    }

    @Test
    void testdataMappers_enumConvertersExposeRawWithSameName() {
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
    void permissionsRequestMappers_toPersonPermissionsGrantRequestRaw_preservesDescription() {
        var rawRequest = PermissionsRequestMappers.toPersonPermissionsGrantRequestRaw(
                PersonPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).invoiceRead().build());
        assertEquals(DESCRIPTION, rawRequest.getDescription());
        assertEquals(NIP, rawRequest.getSubjectIdentifier().getValue());
    }

    @Test
    void permissionsRequestMappers_toEntityPermissionsGrantRequestRaw_preservesPermissionCount() {
        var entityRaw = PermissionsRequestMappers.toEntityPermissionsGrantRequestRaw(
                EntityPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION).entityDetails(FULL_NAME)
                        .invoiceRead().invoiceReadDelegatable().invoiceWrite().invoiceWriteDelegatable().build());
        assertEquals(ENTITY_AUTHORIZATION_PERMISSION_COUNT, entityRaw.getPermissions().size());
        assertEquals(DESCRIPTION, entityRaw.getDescription());
    }

    @Test
    void permissionsRequestMappers_toEntityAuthorizationPermissionsGrantRequestRaw_preservesDescription() {
        var rawRequest = PermissionsRequestMappers.toEntityAuthorizationPermissionsGrantRequestRaw(
                EntityAuthorizationPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .entityDetails(FULL_NAME).selfInvoicing().build());
        assertEquals(DESCRIPTION, rawRequest.getDescription());
    }

    @Test
    void permissionsRequestMappers_toIndirectPermissionsGrantRequestRaw_preservesTargetNip() {
        var rawRequest = PermissionsRequestMappers.toIndirectPermissionsGrantRequestRaw(
                IndirectPermissionGrantBuilder.forNip(NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).invoiceRead().targetNip(OTHER_NIP).build());
        assertEquals(OTHER_NIP, rawRequest.getTargetIdentifier().getValue());
    }

    @Test
    void permissionsRequestMappers_toSubunitPermissionsGrantRequestRaw_preservesContextNip() {
        var rawRequest = PermissionsRequestMappers.toSubunitPermissionsGrantRequestRaw(
                SubunitPermissionGrantBuilder.forNip(NIP).contextNip(OTHER_NIP).description(DESCRIPTION)
                        .personDetails(FIRST_NAME, LAST_NAME).build());
        assertEquals(OTHER_NIP, rawRequest.getContextIdentifier().getValue());
    }

    @Test
    void permissionsRequestMappers_toEuEntityPermissionsGrantRequestRaw_preservesDescription() {
        var rawRequest = PermissionsRequestMappers.toEuEntityPermissionsGrantRequestRaw(
                EuEntityPermissionGrantBuilder.forFingerprint(FINGERPRINT).description(DESCRIPTION)
                        .subjectEntityByFingerprint(FULL_NAME, ADDRESS).invoiceRead().build());
        assertEquals(DESCRIPTION, rawRequest.getDescription());
    }

    @Test
    void permissionsRequestMappers_toEuEntityAdministrationPermissionsGrantRequestRaw_preservesContextVatUe() {
        var rawRequest = PermissionsRequestMappers.toEuEntityAdministrationPermissionsGrantRequestRaw(
                EuEntityAdminPermissionGrantBuilder.forFingerprint(FINGERPRINT)
                        .contextNipVatUe(NIP_VAT_UE).description(DESCRIPTION).euEntityName(EU_ENTITY_NAME)
                        .subjectEntityByFingerprint(FULL_NAME, ADDRESS)
                        .euEntityDetails(EU_ENTITY_NAME, ADDRESS).build());
        assertEquals(NIP_VAT_UE, rawRequest.getContextIdentifier().getValue());
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
    void permissionsQueryRequestMappers_toEntityAuthorizationGranted_preservesAuthorizingIdentifier() {
        var authQuery = PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(
                EntityAuthorizationPermissionsQueryBuilder.granted()
                        .authorizingByNip(NIP).authorizedByNip(OTHER_NIP).selfInvoicing().build());
        assertEquals(NIP, authQuery.getAuthorizingIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toEntityAuthorizationReceived_preservesAuthorizedPeppolId() {
        var authQuery = PermissionsQueryRequestMappers.toEntityAuthorizationPermissionsQueryRequestRaw(
                EntityAuthorizationPermissionsQueryBuilder.received().authorizedByPeppolId(PEPPOL_ID_FIXTURE).build());
        assertEquals(PEPPOL_ID_FIXTURE, authQuery.getAuthorizedIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toPersonPermissionsInCurrentContext_preservesAuthorIdentifier() {
        var personQuery = PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(
                PersonPermissionsQueryBuilder.permissionsInCurrentContext()
                        .authorByNip(NIP).authorizedByPesel(PESEL).contextNip(NIP).targetNip(OTHER_NIP)
                        .invoiceRead().activeOnly().build());
        assertEquals(NIP, personQuery.getAuthorIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toPersonPermissionsGrantedInCurrentContext_preservesAuthorSystem() {
        var personQuery = PermissionsQueryRequestMappers.toPersonPermissionsQueryRequestRaw(
                PersonPermissionsQueryBuilder.permissionsGrantedInCurrentContext().authorSystem().build());
        assertEquals(PersonAuthorIdentifierType.SYSTEM.name(), personQuery.getAuthorIdentifier().getType().name());
    }

    @Test
    void permissionsQueryRequestMappers_toPersonalPermissionsForContextNip_preservesContextIdentifier() {
        var personalQuery = PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create()
                        .contextNip(NIP).targetNip(OTHER_NIP).invoiceRead().vatUeManage().activeOnly().build());
        assertEquals(NIP, personalQuery.getContextIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toPersonalPermissionsForContextInternalId_preservesContextIdentifier() {
        var personalQuery = PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create().contextInternalId(INTERNAL_ID)
                        .targetAllPartners().inactiveOnly().build());
        assertEquals(INTERNAL_ID, personalQuery.getContextIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toPersonalPermissionsForTargetInternalId_preservesTargetIdentifier() {
        var personalQuery = PermissionsQueryRequestMappers.toPersonalPermissionsQueryRequestRaw(
                PersonalPermissionsQueryBuilder.create().targetInternalId(INTERNAL_ID).build());
        assertEquals(INTERNAL_ID, personalQuery.getTargetIdentifier().getValue());
    }

    @Test
    void permissionsQueryRequestMappers_toEuEntityPermissionsQueryRequestRaw_preservesVatUeIdentifier() {
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
