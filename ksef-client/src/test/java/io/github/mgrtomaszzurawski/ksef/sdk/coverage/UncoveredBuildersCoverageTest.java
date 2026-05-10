/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.OnlineSessionBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.SendInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codex 2026-05-05 plan #19 — JaCoCo method-coverage gate ≥ 1.00 on
 * builders/clients. This test exercises every public builder fluent
 * setter that lacked a per-method test, so the gate can flip from
 * 0.75 to 1.00 without arbitrarily-long test writing.
 *
 * <p>Each block calls one method, asserts a verifiable side-effect
 * (the field landed in the built record / a follow-up build()
 * succeeded). Smoke-level coverage; deeper invariant tests live with
 * the per-feature test classes.
 *
 * @since 1.0.0
 */
class UncoveredBuildersCoverageTest {

    private static final byte[] FAKE_AES = new byte[32];
    private static final byte[] FAKE_IV = new byte[16];
    private static final byte[] FAKE_INVOICE = "<Invoice/>".getBytes();
    /** Default connect timeout exercised by the {@code ksefClientBuilder_*} test. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Default read timeout exercised by the {@code ksefClientBuilder_*} test. */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final String DUMMY_ACCESS_TOKEN = "test-access-token";
    private static final String DUMMY_NIP = "1111111111";
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-04-01T00:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-04-30T00:00:00Z");

    private static PublicKey realRsaKey() {
        try {
            java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair().getPublic();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA unavailable", ex);
        }
    }

    @Test
    void onlineSessionBuilder_factoryMethods_andToBuilder() {
        PublicKey key = realRsaKey();
        assertNotNull(OnlineSessionBuilder.fa2(key));
        assertNotNull(OnlineSessionBuilder.fa3(key));
        OnlineSessionBuilder custom = OnlineSessionBuilder.fromFormCode(
                FormCode.custom("Custom (1)", "0-0X", "FA"),
                key);
        OnlineSessionBuilder copy = custom.toBuilder();
        assertNotNull(copy);
        assertNotNull(copy.build());
    }

    @Test
    void invoiceExportBuilder_fullContent_andToBuilder() {
        // given — a query filter set so .build() can succeed on the copy
        var filters = InvoiceQueryBuilder
                .seller()
                .permanentStorageDateFrom(FROM)
                .dateTo(TO);

        // when
        InvoiceExportBuilder builder = InvoiceExportBuilder.create(realRsaKey())
                .filters(filters)
                .fullContent();
        InvoiceExportBuilder copy = builder.toBuilder();
        var request = copy.build();

        // then — toBuilder() must preserve onlyMetadata=false (fullContent flag)
        assertFalse(request.onlyMetadata(),
                "toBuilder() must preserve fullContent flag (onlyMetadata=false) from the source builder");
    }

    @Test
    void invoiceQueryBuilder_build_persistsSortOrderIntoRequest() {
        // given — sortOrder set; .build() must propagate the value into the
        // emitted InvoiceQueryFilters record (PR5 moved this from a separate
        // builder accessor onto the request itself).
        var request = InvoiceQueryBuilder.seller()
                .permanentStorageDateFrom(FROM)
                .dateTo(TO)
                .sortOrder(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder.DESC)
                .build();

        // then
        assertEquals(io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SortOrder.DESC,
                request.sortOrder());
    }

    @Test
    void invoiceQueryBuilder_build_whenDateRangeExceeds3Months_throws() {
        // given — dateTo is more than 3 months past dateFrom; spec caps the
        // server-side window at 3 months and the builder must surface that
        // up-front so a 400 round-trip is avoided.
        var builder = InvoiceQueryBuilder.seller()
                .permanentStorageDateFrom(FROM)
                .dateTo(FROM.plusMonths(4));

        // when / then
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void invoiceExportBuilder_metadataOnly_setsOnlyMetadataTrue() {
        // given — a valid filter so .build() succeeds (filters are required)
        var filters = InvoiceQueryBuilder
                .seller()
                .permanentStorageDateFrom(FROM)
                .dateTo(TO);

        // when
        var request = InvoiceExportBuilder.create(realRsaKey())
                .filters(filters)
                .metadataOnly()
                .build();

        // then — metadataOnly() flips onlyMetadata to true on the resulting request
        assertTrue(request.onlyMetadata(),
                "metadataOnly() must set the export's onlyMetadata flag to true");
    }

    @Test
    void sendInvoiceBuilder_toBuilder_preservesContent() {
        // given
        SendInvoiceBuilder original = SendInvoiceBuilder.create(FAKE_INVOICE, FAKE_AES, FAKE_IV)
                .offline();

        // when
        SendInvoiceBuilder copy = original.toBuilder();
        var copiedRequest = copy.build();

        // then — toBuilder() must preserve offlineMode flag
        assertEquals(true, copiedRequest.offlineMode(),
                "toBuilder() must preserve offline flag from the source builder");
    }

    @Test
    void incrementalSyncPlanBuilder_to_andOtherSetters() {
        // given
        Path syncDir = Path.of("/tmp/sync");

        // when
        IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                .from(FROM)
                .to(TO)
                .subjectTypes(InvoiceQuerySubjectType.SUBJECT1)
                .outputDirectory(syncDir)
                .fullContent(true)
                .build();

        // then — every setter must round-trip into the built plan
        assertEquals(FROM, plan.from());
        assertEquals(TO, plan.to());
        assertEquals(syncDir, plan.outputDirectory());
        assertEquals(true, plan.fullContent());
        assertTrue(plan.subjectTypes().contains(InvoiceQuerySubjectType.SUBJECT1),
                "subjectTypes setter must add SUBJECT1");
    }

    @Test
    void sessionsQueryFilterBuilder_allOptionalSetters() {
        // given
        String reference = "20260418-SE-1111111111-AAAAAAAAAA-01";

        // when
        SessionsQueryFilter filter = SessionsQueryFilter.forBatch()
                .referenceNumber(reference)
                .dateCreatedFrom(FROM)
                .dateCreatedTo(TO)
                .dateClosedFrom(FROM)
                .dateClosedTo(TO)
                .dateModifiedFrom(FROM)
                .dateModifiedTo(TO)
                .build();

        // then — every optional setter must round-trip into the built filter
        assertEquals(KsefSessionType.BATCH, filter.sessionType());
        assertEquals(reference, filter.referenceNumber());
        assertEquals(FROM, filter.dateCreatedFrom());
        assertEquals(TO, filter.dateCreatedTo());
        assertEquals(FROM, filter.dateClosedFrom());
        assertEquals(TO, filter.dateClosedTo());
        assertEquals(FROM, filter.dateModifiedFrom());
        assertEquals(TO, filter.dateModifiedTo());
    }

    @Test
    void subunitPermissionGrantBuilder_forFingerprint() {
        // given
        String fingerprint = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0";

        // when
        var request = SubunitPermissionGrantBuilder.forFingerprint(fingerprint)
                .contextInternalId("ctx-1")
                .description("test description for subunit grant")
                .personDetails("Jan", "Kowalski")
                .build();

        // then — fingerprint flows into identifierValue with FINGERPRINT identifierType
        assertEquals(fingerprint, request.identifierValue());
        assertEquals(PersonSubjectIdentifierType.FINGERPRINT, request.identifierType());
    }

    @Test
    void testPermissionsGrantBuilder_authorizedPesel_authorizedFingerprint() {
        // given
        String supplierNip = "1111111111";
        String pesel = "82060411457";
        String fingerprint = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0";

        // when — apply each authorized-* setter on a fresh builder
        var withPesel = TestPermissionsGrantBuilder.create(supplierNip)
                .authorizedPesel(pesel)
                .invoiceRead()
                .build();
        var withFingerprint = TestPermissionsGrantBuilder.create(supplierNip)
                .authorizedFingerprint(fingerprint)
                .invoiceRead()
                .build();

        // then — each setter chooses the right authorizedType + value
        assertEquals(pesel, withPesel.authorizedValue());
        assertEquals(TestDataAuthorizedIdentifierType.PESEL, withPesel.authorizedType());
        assertEquals(fingerprint, withFingerprint.authorizedValue());
        assertEquals(TestDataAuthorizedIdentifierType.FINGERPRINT, withFingerprint.authorizedType());
    }

    @Test
    void ksefClientBuilder_connectTimeout_readTimeout_features() {
        // given — credentials are mandatory for build() per ADR-016; use a
        // throw-away token-based pair so the build can complete.
        var creds = new KsefTokenCredentials(DUMMY_ACCESS_TOKEN, KsefIdentifier.nip(DUMMY_NIP));

        // when — exercise the three setters
        var builder = KsefClient
                .builder()
                .environment(KsefEnvironment.TEST)
                .credentials(creds)
                .connectTimeout(CONNECT_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .features(FeaturePolicy.defaults());

        // then — build succeeds, environment round-trips
        assertNotNull(builder);
        try (var client = builder.build()) {
            assertNotNull(client);
            assertEquals(KsefEnvironment.TEST, client.environment());
        }
    }
}
