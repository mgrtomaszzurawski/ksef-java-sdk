/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.coverage;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateQueryRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.KsefCertificateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryDateType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQueryFilters;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceQuerySubjectType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoicingMode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.OnlineSessionOpenRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.SendInvoiceRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenGenerateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.tokens.model.TokenPermissionType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FormCodeInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefSessionTerminalFailureException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecordsEqualsHashCodeTest {

    @Test
    void certificateEnrollRequest_equalsHashCodeToString() {
        var a = new CertificateEnrollRequest("c", KsefCertificateType.AUTHENTICATION, new byte[]{1, 2}, null);
        var b = new CertificateEnrollRequest("c", KsefCertificateType.AUTHENTICATION, new byte[]{1, 2}, null);
        var c = new CertificateEnrollRequest("d", KsefCertificateType.OFFLINE, new byte[]{3}, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, null);
        assertNotEquals(a, "string");
        assertEquals(a, a);
    }

    @Test
    void recordsToStringNonNull() {
        assertNotNull(new CertificateQueryRequest(null, null, null, null, null).toString());
        assertNotNull(new CertificateQueryRequest("s", "n", KsefCertificateType.AUTHENTICATION,
                CertificateStatus.ACTIVE, java.time.OffsetDateTime.now()).toString());
        assertNotNull(new TokenGenerateRequest("d", List.of(TokenPermissionType.INVOICE_READ)).toString());
        assertNotNull(new InvoiceQueryFilters(InvoiceQuerySubjectType.SUBJECT1, InvoiceQueryDateType.INVOICING,
                java.time.OffsetDateTime.now(), null, null, null, null, InvoicingMode.ONLINE, null, null).toString());
        assertNotNull(new SendInvoiceRequest(new byte[]{1}, 1L, new byte[]{2}, 2L, new byte[]{3}, false).toString());
        var session = new OnlineSessionOpenRequest(new FormCodeInfo("FA", "2", "FA (2)"),
                new byte[]{1}, new byte[]{2}, new byte[]{3});
        assertNotNull(session.toString());
        assertEquals(session, session);
        var session2 = new OnlineSessionOpenRequest(new FormCodeInfo("FA", "2", "FA (2)"),
                new byte[]{1}, new byte[]{2}, new byte[]{3});
        assertEquals(session, session2);
        assertNotEquals(session, null);
        assertNotEquals(session, "x");
        assertEquals(session.hashCode(), session2.hashCode());
    }

    @Test
    void terminalFailureException_accessors() {
        var e = new KsefSessionTerminalFailureException("ref", 415, "desc", List.of("d1"));
        assertEquals("ref", e.referenceNumber());
        assertEquals(415, e.code());
        assertEquals("desc", e.description());
        assertEquals(List.of("d1"), e.details());
        assertNotNull(e.getMessage());
        assertNotNull(new KsefSessionTerminalFailureException("ref", 415, null, null).details());
    }
}
