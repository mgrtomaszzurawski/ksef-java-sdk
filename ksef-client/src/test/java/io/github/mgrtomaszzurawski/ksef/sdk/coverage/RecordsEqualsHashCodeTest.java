/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionOpenRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of auto-generated record equals/hashCode/toString plus the
 * KsefSessionTerminalFailureException accessors. Each method exercises one
 * record's contract independently.
 */
class RecordsEqualsHashCodeTest {

    private static final int TERMINAL_STATUS_CODE = 415;
    private static final String CERT_NAME_LEFT = "left-cert";
    private static final String CERT_NAME_RIGHT = "right-cert";
    private static final String QUERY_SERIAL = "serial-1";
    private static final String QUERY_NAME = "name-1";
    private static final String TOKEN_DESCRIPTION = "token-desc";
    private static final String SYSTEM_CODE = "FA";
    private static final String SCHEMA_VERSION = "2";
    private static final String FORM_CODE_VALUE = "FA (2)";
    private static final byte[] FAKE_CSR = new byte[]{1, 2};
    private static final byte[] FAKE_CSR_OTHER = new byte[]{3};
    private static final byte[] FAKE_BYTES_ONE = new byte[]{1};
    private static final byte[] FAKE_BYTES_TWO = new byte[]{2};
    private static final byte[] FAKE_BYTES_THREE = new byte[]{3};
    private static final long FAKE_INVOICE_SIZE = 1L;
    private static final long FAKE_ENCRYPTED_SIZE = 2L;
    private static final String SESSION_REF = "ref-1";
    private static final String FAILURE_DESCRIPTION = "Schema validation rejected";
    private static final String FAILURE_DETAIL = "field 'X' is invalid";
    private static final String EXPECTED_OFFLINE_MODE_FRAGMENT = "offlineMode=false";

    @Test
    void certificateEnrollRequest_equalsAndHashCode_treatStructurallyEqualBytesAsEqual() {
        CertificateEnrollRequest left = new CertificateEnrollRequest(
                CERT_NAME_LEFT, KsefCertificateType.AUTHENTICATION, FAKE_CSR, null);
        CertificateEnrollRequest leftCopy = new CertificateEnrollRequest(
                CERT_NAME_LEFT, KsefCertificateType.AUTHENTICATION, FAKE_CSR, null);
        CertificateEnrollRequest right = new CertificateEnrollRequest(
                CERT_NAME_RIGHT, KsefCertificateType.OFFLINE, FAKE_CSR_OTHER, null);
        assertEquals(left, leftCopy);
        assertEquals(left.hashCode(), leftCopy.hashCode());
        assertNotEquals(left, right);
        assertNotEquals(left, null);
        assertNotEquals(left, "not-a-record");
        assertEquals(left, left);
    }

    @Test
    void certificateQueryRequest_toString_doesNotThrowOnNullFields() {
        assertNotNull(new CertificateQueryRequest(null, null, null, null, null).toString());
    }

    @Test
    void certificateQueryRequest_toString_includesSerialAndName() {
        String rendered = new CertificateQueryRequest(QUERY_SERIAL, QUERY_NAME,
                KsefCertificateType.AUTHENTICATION, CertificateStatus.ACTIVE,
                OffsetDateTime.now()).toString();
        assertTrue(rendered.contains(QUERY_SERIAL));
        assertTrue(rendered.contains(QUERY_NAME));
    }

    @Test
    void tokenGenerateRequest_toString_includesPermissionsList() {
        String rendered = new TokenGenerateRequest(TOKEN_DESCRIPTION,
                List.of(TokenPermissionType.INVOICE_READ)).toString();
        assertTrue(rendered.contains(TokenPermissionType.INVOICE_READ.name()));
        assertTrue(rendered.contains(TOKEN_DESCRIPTION));
    }

    @Test
    void invoiceQueryFilters_toString_includesSubjectTypeAndInvoicingMode() {
        String rendered = new InvoiceQueryFilters(
                InvoiceQuerySubjectType.SUBJECT1, InvoiceQueryDateType.INVOICING,
                OffsetDateTime.now(), null, null, null, null,
                InvoicingMode.ONLINE, null, null).toString();
        assertTrue(rendered.contains(InvoiceQuerySubjectType.SUBJECT1.name()));
        assertTrue(rendered.contains(InvoicingMode.ONLINE.name()));
    }

    @Test
    void sendInvoiceRequest_toString_includesInvoiceSizeAndOfflineMode() {
        String rendered = new SendInvoiceRequest(FAKE_BYTES_ONE, FAKE_INVOICE_SIZE,
                FAKE_BYTES_TWO, FAKE_ENCRYPTED_SIZE, FAKE_BYTES_THREE, false).toString();
        assertTrue(rendered.contains(String.valueOf(FAKE_INVOICE_SIZE)));
        assertTrue(rendered.contains(EXPECTED_OFFLINE_MODE_FRAGMENT));
    }

    @Test
    void onlineSessionOpenRequest_equalsAndHashCode_byteFieldsAreStructurallyCompared() {
        FormCodeInfo formCode = new FormCodeInfo(SYSTEM_CODE, SCHEMA_VERSION, FORM_CODE_VALUE);
        OnlineSessionOpenRequest left = new OnlineSessionOpenRequest(formCode,
                FAKE_BYTES_ONE, FAKE_BYTES_TWO, FAKE_BYTES_THREE);
        OnlineSessionOpenRequest right = new OnlineSessionOpenRequest(formCode,
                FAKE_BYTES_ONE, FAKE_BYTES_TWO, FAKE_BYTES_THREE);
        assertEquals(left, left);
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, null);
        assertNotEquals(left, "not-a-record");
    }

    @Test
    void onlineSessionOpenRequest_toString_includesFormCodeValue() {
        FormCodeInfo formCode = new FormCodeInfo(SYSTEM_CODE, SCHEMA_VERSION, FORM_CODE_VALUE);
        OnlineSessionOpenRequest request = new OnlineSessionOpenRequest(formCode,
                FAKE_BYTES_ONE, FAKE_BYTES_TWO, FAKE_BYTES_THREE);
        String rendered = request.toString();
        assertTrue(rendered.contains(SYSTEM_CODE));
        assertTrue(rendered.contains(SCHEMA_VERSION));
    }

    @Test
    void terminalFailureException_accessors_returnConstructorValues() {
        KsefSessionTerminalFailureException failure = new KsefSessionTerminalFailureException(
                SESSION_REF, TERMINAL_STATUS_CODE, FAILURE_DESCRIPTION, List.of(FAILURE_DETAIL));
        assertEquals(SESSION_REF, failure.referenceNumber());
        assertEquals(TERMINAL_STATUS_CODE, failure.code());
        assertEquals(FAILURE_DESCRIPTION, failure.description());
        assertEquals(List.of(FAILURE_DETAIL), failure.details());
        assertNotNull(failure.getMessage());
    }

    @Test
    void terminalFailureException_whenDetailsNull_returnsEmptyList() {
        KsefSessionTerminalFailureException failure = new KsefSessionTerminalFailureException(
                SESSION_REF, TERMINAL_STATUS_CODE, null, null);
        assertEquals(List.of(), failure.details());
    }
}
