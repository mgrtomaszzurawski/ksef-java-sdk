/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openapitools.jackson.nullable.JsonNullableModule;
import io.github.mgrtomaszzurawski.ksef.client.model.BatchSessionEffectiveContextLimitsRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BuyerIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataBuyerRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceMetadataThirdSubjectRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackagePartRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicePackageRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceStatusInfoRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoiceTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.InvoicingModeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.ThirdSubjectIdentifierTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UpoPageResponseRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing.model.BatchSessionLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BuyerIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceBuyer;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackage;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicePackagePart;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceStatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceThirdSubject;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.ThirdSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoPage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Branch coverage for {@link InvoicingMappers} singular-record converters.
 *
 * <p>The plural wrappers (e.g. {@code toInvoiceMetadataResult}) are exercised
 * by {@code InvoiceClientTest} with empty stub responses, leaving the
 * singular per-element mappers (and their null-coalescing branches) at 0%
 * coverage. This suite drives them directly.
 *
 * <p>Driven from JSON via the same {@link ObjectMapper} the runtime uses,
 * which also exercises the {@code @JsonProperty} annotations on each Raw.
 */
class InvoicingMappersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new JsonNullableModule());

    private static final String KSEF_NUMBER = "20260404-FA-0000000000-AAAAAAAAAA-12";
    private static final String INVOICE_NUMBER = "FV/2026/04/001";
    private static final String NIP = "1111111111";
    private static final String SELLER_NAME = "Acme Sp. z o.o.";
    private static final String BUYER_NAME = "Buyer Co.";
    private static final String SUBJECT_NAME = "Third Party";
    /** {@code role} on {@code InvoiceMetadataThirdSubjectRaw} is {@code Integer}, not a free string. */
    private static final int SUBJECT_ROLE = 3;
    private static final String CURRENCY = "PLN";
    private static final String DATE_ISO = "2026-04-15T10:00:00+02:00";
    /** {@code issueDate} on {@code InvoiceMetadataRaw} is {@code LocalDate}, not {@code OffsetDateTime}. */
    private static final String LOCAL_DATE_ISO = "2026-04-15";
    private static final String INVOICE_HASH = "abc123==";
    private static final String NET_AMOUNT = "1000.00";
    private static final String GROSS_AMOUNT = "1230.00";
    private static final String VAT_AMOUNT = "230.00";
    private static final long INVOICE_COUNT = 100L;
    private static final long PACKAGE_SIZE = 10485760L;
    private static final int PART_ORDINAL = 1;
    private static final long PART_SIZE = 524288L;
    private static final long ENCRYPTED_PART_SIZE = 524304L;
    private static final String PART_NAME = "part-1.bin";
    private static final String PART_HASH = "deadbeef";
    private static final String ENCRYPTED_PART_HASH = "cafebabe";
    private static final String PART_URL = "https://upload.example.com/part1";
    private static final String PART_METHOD = "PUT";
    private static final String UPO_REF = "20260404-UPO-0000000000-AAAAAAAAAA-77";
    private static final String UPO_URL = "https://download.example.com/upo";
    private static final int MAX_INVOICE_SIZE_MB = 10;
    private static final int MAX_INVOICE_WITH_ATTACH_MB = 50;
    private static final int MAX_INVOICES = 1000;
    private static final int STATUS_CODE_OK = 200;
    private static final String STATUS_DESCRIPTION = "OK";

    @Test
    void toBatchSessionLimits_mapsAllFields() throws Exception {
        // given
        String json = """
                {"maxInvoiceSizeInMB": %d, "maxInvoiceWithAttachmentSizeInMB": %d, "maxInvoices": %d}
                """.formatted(MAX_INVOICE_SIZE_MB, MAX_INVOICE_WITH_ATTACH_MB, MAX_INVOICES);
        BatchSessionEffectiveContextLimitsRaw raw =
                OBJECT_MAPPER.readValue(json, BatchSessionEffectiveContextLimitsRaw.class);

        // when
        BatchSessionLimits result = InvoicingMappers.toBatchSessionLimits(raw);

        // then
        assertEquals(MAX_INVOICE_SIZE_MB, result.maxInvoiceSizeInMB());
        assertEquals(MAX_INVOICE_WITH_ATTACH_MB, result.maxInvoiceWithAttachmentSizeInMB());
        assertEquals(MAX_INVOICES, result.maxInvoices());
    }

    @Test
    void toBatchSessionLimits_nullInput_yieldsNull() {
        // when / then
        assertNull(InvoicingMappers.toBatchSessionLimits(null));
    }

    @Test
    void toBuyerIdentifierType_coversAllEnumBranches() {
        // when / then
        assertEquals(BuyerIdentifierType.NIP, InvoicingMappers.toBuyerIdentifierType(BuyerIdentifierTypeRaw.NIP));
        assertEquals(BuyerIdentifierType.VAT_UE, InvoicingMappers.toBuyerIdentifierType(BuyerIdentifierTypeRaw.VAT_UE));
        assertEquals(BuyerIdentifierType.OTHER, InvoicingMappers.toBuyerIdentifierType(BuyerIdentifierTypeRaw.OTHER));
        assertEquals(BuyerIdentifierType.NONE, InvoicingMappers.toBuyerIdentifierType(BuyerIdentifierTypeRaw.NONE));
        assertNull(InvoicingMappers.toBuyerIdentifierType(null));
    }

    @Test
    void toThirdSubjectIdentifierType_coversAllEnumBranches() {
        // when / then
        assertEquals(ThirdSubjectIdentifierType.NIP, InvoicingMappers.toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw.NIP));
        assertEquals(ThirdSubjectIdentifierType.INTERNAL_ID, InvoicingMappers.toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw.INTERNAL_ID));
        assertEquals(ThirdSubjectIdentifierType.VAT_UE, InvoicingMappers.toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw.VAT_UE));
        assertEquals(ThirdSubjectIdentifierType.OTHER, InvoicingMappers.toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw.OTHER));
        assertEquals(ThirdSubjectIdentifierType.NONE, InvoicingMappers.toThirdSubjectIdentifierType(ThirdSubjectIdentifierTypeRaw.NONE));
        assertNull(InvoicingMappers.toThirdSubjectIdentifierType(null));
    }

    @Test
    void toInvoiceType_coversAllEnumBranches() {
        // when / then
        for (InvoiceTypeRaw raw : InvoiceTypeRaw.values()) {
            assertNotNull(InvoicingMappers.toInvoiceType(raw),
                    "missing branch for InvoiceTypeRaw." + raw);
        }
        assertNull(InvoicingMappers.toInvoiceType(null));
        assertEquals(InvoiceType.VAT, InvoicingMappers.toInvoiceType(InvoiceTypeRaw.VAT));
        assertEquals(InvoiceType.KOR, InvoicingMappers.toInvoiceType(InvoiceTypeRaw.KOR));
    }

    @Test
    void toInvoicingMode_coversAllEnumBranches() {
        // when / then
        assertEquals(InvoicingMode.ONLINE, InvoicingMappers.toInvoicingMode(InvoicingModeRaw.ONLINE));
        assertEquals(InvoicingMode.OFFLINE, InvoicingMappers.toInvoicingMode(InvoicingModeRaw.OFFLINE));
        assertNull(InvoicingMappers.toInvoicingMode(null));
    }

    @Test
    void toFormCodeInfo_mapsAllFields() throws Exception {
        // given
        String json = """
                {"systemCode": "FA (2)", "schemaVersion": "1-0E", "value": "FA"}
                """;
        FormCodeRaw raw = OBJECT_MAPPER.readValue(json, FormCodeRaw.class);

        // when
        FormCodeInfo result = InvoicingMappers.toFormCodeInfo(raw);

        // then
        assertEquals("FA (2)", result.systemCode());
        assertEquals("1-0E", result.schemaVersion());
        assertEquals("FA", result.value());
    }

    @Test
    void toFormCodeInfo_nullInput_yieldsNull() {
        // when / then
        assertNull(InvoicingMappers.toFormCodeInfo(null));
    }

    @Test
    void toInvoiceBuyer_withIdentifier_mapsAllFields() throws Exception {
        // given
        String json = """
                {"identifier": {"type": "Nip", "value": "%s"}, "name": "%s"}
                """.formatted(NIP, BUYER_NAME);
        InvoiceMetadataBuyerRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataBuyerRaw.class);

        // when
        InvoiceBuyer result = InvoicingMappers.toInvoiceBuyer(raw);

        // then
        assertEquals(BuyerIdentifierType.NIP, result.identifierType());
        assertEquals(NIP, result.identifierValue());
        assertEquals(BUYER_NAME, result.name());
    }

    @Test
    void toInvoiceBuyer_withoutIdentifier_yieldsNullIdFields() throws Exception {
        // given
        String json = """
                {"identifier": null, "name": "%s"}
                """.formatted(BUYER_NAME);
        InvoiceMetadataBuyerRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataBuyerRaw.class);

        // when
        InvoiceBuyer result = InvoicingMappers.toInvoiceBuyer(raw);

        // then
        assertNull(result.identifierType());
        assertNull(result.identifierValue());
        assertEquals(BUYER_NAME, result.name());
    }

    @Test
    void toInvoiceBuyer_nullInput_yieldsNull() {
        // when / then
        assertNull(InvoicingMappers.toInvoiceBuyer(null));
    }

    @Test
    void toInvoiceThirdSubject_withIdentifier_mapsAllFields() throws Exception {
        // given
        String json = """
                {"identifier": {"type": "Nip", "value": "%s"}, "name": "%s", "role": %d}
                """.formatted(NIP, SUBJECT_NAME, SUBJECT_ROLE);
        InvoiceMetadataThirdSubjectRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataThirdSubjectRaw.class);

        // when
        InvoiceThirdSubject result = InvoicingMappers.toInvoiceThirdSubject(raw);

        // then
        assertEquals(ThirdSubjectIdentifierType.NIP, result.identifierType());
        assertEquals(NIP, result.identifierValue());
        assertEquals(SUBJECT_NAME, result.name());
        assertEquals(SUBJECT_ROLE, result.role().intValue());
    }

    @Test
    void toInvoiceThirdSubject_withoutIdentifier_yieldsNullIdFields() throws Exception {
        // given
        String json = """
                {"identifier": null, "name": "%s", "role": %d}
                """.formatted(SUBJECT_NAME, SUBJECT_ROLE);
        InvoiceMetadataThirdSubjectRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataThirdSubjectRaw.class);

        // when
        InvoiceThirdSubject result = InvoicingMappers.toInvoiceThirdSubject(raw);

        // then
        assertNull(result.identifierType());
        assertNull(result.identifierValue());
        assertEquals(SUBJECT_NAME, result.name());
    }

    @Test
    void toInvoicePackagePart_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "ordinalNumber": %d,
                  "partName": "%s",
                  "method": "%s",
                  "url": "%s",
                  "partSize": %d,
                  "partHash": "%s",
                  "encryptedPartSize": %d,
                  "encryptedPartHash": "%s",
                  "expirationDate": "%s"
                }""".formatted(PART_ORDINAL, PART_NAME, PART_METHOD, PART_URL,
                        PART_SIZE, PART_HASH, ENCRYPTED_PART_SIZE, ENCRYPTED_PART_HASH, DATE_ISO);
        InvoicePackagePartRaw raw = OBJECT_MAPPER.readValue(json, InvoicePackagePartRaw.class);

        // when
        InvoicePackagePart result = InvoicingMappers.toInvoicePackagePart(raw);

        // then
        assertEquals(PART_ORDINAL, result.ordinalNumber());
        assertEquals(PART_NAME, result.partName());
        assertEquals(PART_METHOD, result.method());
        assertEquals(java.net.URI.create(PART_URL), result.url());
        assertEquals(PART_SIZE, result.partSize());
        assertEquals(ENCRYPTED_PART_SIZE, result.encryptedPartSize());
        assertNotNull(result.partHash());
        assertNotNull(result.encryptedPartHash());
    }

    @Test
    void toInvoicePackage_mapsAllFieldsWithParts() throws Exception {
        // given
        String json = """
                {
                  "invoiceCount": %d,
                  "size": %d,
                  "parts": [
                    {
                      "ordinalNumber": %d,
                      "partName": "%s",
                      "method": "%s",
                      "url": "%s",
                      "partSize": %d,
                      "partHash": "%s",
                      "encryptedPartSize": %d,
                      "encryptedPartHash": "%s",
                      "expirationDate": "%s"
                    }
                  ],
                  "isTruncated": false,
                  "lastIssueDate": "%s",
                  "lastInvoicingDate": "%s",
                  "lastPermanentStorageDate": "%s",
                  "permanentStorageHwmDate": "%s"
                }""".formatted(INVOICE_COUNT, PACKAGE_SIZE,
                        PART_ORDINAL, PART_NAME, PART_METHOD, PART_URL,
                        PART_SIZE, PART_HASH, ENCRYPTED_PART_SIZE, ENCRYPTED_PART_HASH, DATE_ISO,
                        LOCAL_DATE_ISO, DATE_ISO, DATE_ISO, DATE_ISO);
        InvoicePackageRaw raw = OBJECT_MAPPER.readValue(json, InvoicePackageRaw.class);

        // when
        InvoicePackage result = InvoicingMappers.toInvoicePackage(raw);

        // then
        assertEquals(INVOICE_COUNT, result.invoiceCount());
        assertEquals(PACKAGE_SIZE, result.size());
        assertEquals(1, result.parts().size());
    }

    @Test
    void toInvoicePackage_nullInput_yieldsNull() {
        // when / then
        assertNull(InvoicingMappers.toInvoicePackage(null));
    }

    @Test
    void toInvoiceStatusInfo_withDetailsAndExtensions_mapsAllFields() throws Exception {
        // given
        String json = """
                {
                  "code": %d,
                  "description": "%s",
                  "details": ["d1", "d2"],
                  "extensions": {"k1": "v1"}
                }""".formatted(STATUS_CODE_OK, STATUS_DESCRIPTION);
        InvoiceStatusInfoRaw raw = OBJECT_MAPPER.readValue(json, InvoiceStatusInfoRaw.class);

        // when
        InvoiceStatusInfo result = InvoicingMappers.toInvoiceStatusInfo(raw);

        // then
        assertEquals(STATUS_CODE_OK, result.code());
        assertEquals(STATUS_DESCRIPTION, result.description());
        assertEquals(2, result.details().size());
        assertEquals("v1", result.extensions().get("k1"));
    }

    @Test
    void toInvoiceStatusInfo_withoutDetailsAndExtensions_yieldsEmptyCollections() throws Exception {
        // given
        String json = """
                {"code": %d, "description": "%s", "details": null, "extensions": null}
                """.formatted(STATUS_CODE_OK, STATUS_DESCRIPTION);
        InvoiceStatusInfoRaw raw = OBJECT_MAPPER.readValue(json, InvoiceStatusInfoRaw.class);

        // when
        InvoiceStatusInfo result = InvoicingMappers.toInvoiceStatusInfo(raw);

        // then
        assertEquals(0, result.details().size());
        assertEquals(0, result.extensions().size());
    }

    @Test
    void toInvoiceStatusInfo_nullInput_yieldsNull() {
        // when / then
        assertNull(InvoicingMappers.toInvoiceStatusInfo(null));
    }

    @Test
    void toUpoPage_mapsAllFields() throws Exception {
        // given
        String json = """
                {"referenceNumber": "%s", "downloadUrl": "%s", "downloadUrlExpirationDate": "%s"}
                """.formatted(UPO_REF, UPO_URL, DATE_ISO);
        UpoPageResponseRaw raw = OBJECT_MAPPER.readValue(json, UpoPageResponseRaw.class);

        // when
        UpoPage result = InvoicingMappers.toUpoPage(raw);

        // then
        assertEquals(UPO_REF, result.referenceNumber());
        assertEquals(java.net.URI.create(UPO_URL), result.downloadUrl());
    }

    @Test
    void toInvoiceMetadata_withSellerBuyerSubjectsAndAllOptional_mapsEveryField() throws Exception {
        // given
        String json = """
                {
                  "ksefNumber": "%s",
                  "invoiceNumber": "%s",
                  "issueDate": "%s",
                  "invoicingDate": "%s",
                  "acquisitionDate": "%s",
                  "permanentStorageDate": "%s",
                  "seller": {"nip": "%s", "name": "%s"},
                  "buyer": {"identifier": {"type": "Nip", "value": "%s"}, "name": "%s"},
                  "netAmount": "%s",
                  "grossAmount": "%s",
                  "vatAmount": "%s",
                  "currency": "%s",
                  "invoicingMode": "Online",
                  "invoiceType": "Vat",
                  "formCode": {"systemCode": "FA (2)", "schemaVersion": "1-0E", "value": "FA"},
                  "isSelfInvoicing": false,
                  "hasAttachment": false,
                  "invoiceHash": "%s",
                  "hashOfCorrectedInvoice": null,
                  "thirdSubjects": [
                    {"identifier": {"type": "Nip", "value": "%s"}, "name": "%s", "role": %d}
                  ]
                }""".formatted(KSEF_NUMBER, INVOICE_NUMBER, LOCAL_DATE_ISO, DATE_ISO, DATE_ISO, DATE_ISO,
                        NIP, SELLER_NAME,
                        NIP, BUYER_NAME,
                        NET_AMOUNT, GROSS_AMOUNT, VAT_AMOUNT, CURRENCY,
                        INVOICE_HASH,
                        NIP, SUBJECT_NAME, SUBJECT_ROLE);
        InvoiceMetadataRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataRaw.class);

        // when
        InvoiceMetadata result = InvoicingMappers.toInvoiceMetadata(raw);

        // then
        assertEquals(KSEF_NUMBER, result.ksefNumber());
        assertEquals(INVOICE_NUMBER, result.invoiceNumber());
        assertNotNull(result.seller());
        assertEquals(SELLER_NAME, result.seller().name());
        assertNotNull(result.buyer());
        assertEquals(InvoiceType.VAT, result.invoiceType());
        assertEquals(InvoicingMode.ONLINE, result.invoicingMode());
        assertNotNull(result.formCode());
        assertEquals(1, result.thirdSubjects().size());
    }

    @Test
    void toInvoiceMetadata_withoutThirdSubjects_yieldsEmptyList() throws Exception {
        // given
        String json = """
                {
                  "ksefNumber": "%s",
                  "invoiceNumber": "%s",
                  "issueDate": "%s",
                  "invoicingDate": "%s",
                  "acquisitionDate": "%s",
                  "permanentStorageDate": "%s",
                  "seller": {"nip": "%s", "name": "%s"},
                  "buyer": null,
                  "netAmount": "%s",
                  "grossAmount": "%s",
                  "vatAmount": "%s",
                  "currency": "%s",
                  "invoicingMode": "Online",
                  "invoiceType": "Vat",
                  "formCode": null,
                  "isSelfInvoicing": false,
                  "hasAttachment": false,
                  "invoiceHash": "%s",
                  "hashOfCorrectedInvoice": null,
                  "thirdSubjects": null
                }""".formatted(KSEF_NUMBER, INVOICE_NUMBER, LOCAL_DATE_ISO, DATE_ISO, DATE_ISO, DATE_ISO,
                        NIP, SELLER_NAME,
                        NET_AMOUNT, GROSS_AMOUNT, VAT_AMOUNT, CURRENCY,
                        INVOICE_HASH);
        InvoiceMetadataRaw raw = OBJECT_MAPPER.readValue(json, InvoiceMetadataRaw.class);

        // when
        InvoiceMetadata result = InvoicingMappers.toInvoiceMetadata(raw);

        // then
        assertEquals(0, result.thirdSubjects().size());
        assertNull(result.buyer());
        assertNull(result.formCode());
    }
}
