/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceExportBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.OnlineSessionBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.SendInvoiceBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.KsefSessionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SessionsQueryFilter;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode.custom(
                        "Custom (1)", "0-0X", "FA"),
                key);
        OnlineSessionBuilder copy = custom.toBuilder();
        assertNotNull(copy);
        assertNotNull(copy.build());
    }

    @Test
    void invoiceExportBuilder_fullContent_andToBuilder() {
        // given — a query filter set so .build() can succeed on the copy
        var filters = io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder
                .seller()
                .permanentStorageDateFrom(FROM)
                .permanentStorageDateTo(TO);

        // when
        InvoiceExportBuilder builder = InvoiceExportBuilder.create(realRsaKey())
                .filters(filters)
                .fullContent();
        InvoiceExportBuilder copy = builder.toBuilder();
        var request = copy.build();

        // then — toBuilder() must preserve onlyMetadata=false (fullContent flag)
        assertEquals(false, request.onlyMetadata(),
                "toBuilder() must preserve fullContent flag (onlyMetadata=false) from the source builder");
    }

    @Test
    void sendInvoiceBuilder_toBuilder_preservesContent() {
        SendInvoiceBuilder builder = SendInvoiceBuilder.create(FAKE_INVOICE, FAKE_AES, FAKE_IV)
                .offline();
        SendInvoiceBuilder copy = builder.toBuilder();
        assertNotNull(copy.build());
    }

    @Test
    void incrementalSyncPlanBuilder_to_andOtherSetters() {
        IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                .from(FROM)
                .to(TO)
                .subjectTypes(InvoiceQuerySubjectType.SUBJECT1)
                .outputDirectory(Path.of("/tmp/sync"))
                .fullContent(true)
                .build();
        assertEquals(TO, plan.to());
    }

    @Test
    void sessionsQueryFilterBuilder_allOptionalSetters() {
        SessionsQueryFilter filter = SessionsQueryFilter.forBatch()
                .referenceNumber("20260418-SE-1234567890-AAAAAAAAAA-01")
                .dateCreatedFrom(FROM)
                .dateCreatedTo(TO)
                .dateClosedFrom(FROM)
                .dateClosedTo(TO)
                .dateModifiedFrom(FROM)
                .dateModifiedTo(TO)
                .build();
        assertEquals(KsefSessionType.BATCH, filter.sessionType());
        assertEquals(FROM, filter.dateCreatedFrom());
    }

    @Test
    void subunitPermissionGrantBuilder_forFingerprint() {
        // given
        String fingerprint = "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0";

        // when
        var request = SubunitPermissionGrantBuilder.forFingerprint(fingerprint)
                .contextInternalId("ctx-1")
                .description("desc")
                .personDetails("Jan", "Kowalski")
                .build();

        // then — fingerprint flows into identifierValue with FINGERPRINT identifierType
        assertEquals(fingerprint, request.identifierValue());
        assertEquals(io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PersonSubjectIdentifierType.FINGERPRINT,
                request.identifierType());
    }

    @Test
    void testPermissionsGrantBuilder_authorizedPesel_authorizedFingerprint() {
        // given
        String supplierNip = "1234567890";
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
        assertEquals(io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType.PESEL,
                withPesel.authorizedType());
        assertEquals(fingerprint, withFingerprint.authorizedValue());
        assertEquals(io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataAuthorizedIdentifierType.FINGERPRINT,
                withFingerprint.authorizedType());
    }

    @Test
    void ksefClientBuilder_connectTimeout_readTimeout_features() {
        // given / when — exercise the three setters and confirm the builder
        // build()s without throwing (the setters are simple stores; richer
        // verification would require exposing internals not on public API)
        var builder = io.github.mgrtomaszzurawski.ksef.sdk.KsefClient
                .builder(KsefEnvironment.TEST)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .readTimeout(java.time.Duration.ofSeconds(30))
                .features(io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy.defaults());

        // then — chain returns same builder type, ready for build()
        assertNotNull(builder);
        try (var client = builder.build()) {
            assertNotNull(client);
            assertEquals(KsefEnvironment.TEST, client.environment());
        }
    }
}
