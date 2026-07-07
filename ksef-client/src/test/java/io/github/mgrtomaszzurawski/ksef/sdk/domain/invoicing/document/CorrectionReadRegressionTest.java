/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.InvoiceCorrectionReference;
import io.github.mgrtomaszzurawski.ksef.sdk.testfixtures.Fa3InvoiceFixtures;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import javax.xml.datatype.DatatypeFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Read-side regression guard for correction metadata. The document
 * previously exposed only {@code invoiceTypeCode}; the {@code DaneFaKorygowanej}
 * reference and the {@code PrzyczynaKorekty} / {@code TypKorekty} /
 * {@code OkresFaKorygowanej} context were reachable only via the JAXB escape
 * hatch, so a consumer could not read which invoice a correction corrects
 * through the typed surface.
 */
class CorrectionReadRegressionTest {

    private static final String CORRECTED_NUMBER = "FA/2026/ORIGINAL/0007";
    private static final String CORRECTED_KSEF = "1111111111-20260501-AABBCC000000-11";
    private static final LocalDate CORRECTED_DATE = LocalDate.of(2026, 5, 1);
    private static final String REASON = "Blad ceny jednostkowej";
    private static final BigInteger TYPE = BigInteger.valueOf(1);
    private static final String PERIOD = "2026-05";

    @Test
    void fa3_whenOriginalInvoice_correctionAccessorsAreEmpty() throws Exception {
        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(Fa3InvoiceFixtures.minimalValid().xml());

        assertTrue(doc.correctedInvoices().isEmpty(), "original invoice has no corrected refs");
        assertNull(doc.correctionReason(), "original invoice has no correction reason");
        assertNull(doc.correctionType(), "original invoice has no correction type");
        assertNull(doc.correctedPeriod(), "original invoice has no corrected period");
    }

    @Test
    void fa3_whenCorrection_exposesCorrectedInvoiceAndContext() throws Exception {
        io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura faktura =
                unmarshal(Fa3InvoiceFixtures.minimalValid().xml());
        io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa fa = faktura.getFa();
        fa.setPrzyczynaKorekty(REASON);
        fa.setTypKorekty(TYPE);
        fa.setOkresFaKorygowanej(PERIOD);

        io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.DaneFaKorygowanej dane =
                new io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.Fa.DaneFaKorygowanej();
        dane.setNrFaKorygowanej(CORRECTED_NUMBER);
        dane.setDataWystFaKorygowanej(DatatypeFactory.newInstance()
                .newXMLGregorianCalendarDate(2026, 5, 1, javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED));
        dane.setNrKSeFFaKorygowanej(CORRECTED_KSEF);
        fa.getDaneFaKorygowanej().add(dane);

        Fa3InvoiceDocument doc = Fa3InvoiceDocument.from(marshal(faktura));

        assertEquals(1, doc.correctedInvoices().size());
        InvoiceCorrectionReference ref = doc.correctedInvoices().get(0);
        assertEquals(CORRECTED_NUMBER, ref.originalInvoiceNumber());
        assertEquals(CORRECTED_DATE, ref.originalInvoiceDate());
        assertEquals(CORRECTED_KSEF, ref.originalKsefNumber());
        assertEquals(REASON, doc.correctionReason());
        assertEquals(1, doc.correctionType());
        assertEquals(PERIOD, doc.correctedPeriod());
    }

    private static io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura unmarshal(byte[] xml) throws Exception {
        Unmarshaller unmarshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.class).createUnmarshaller();
        return (io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura)
                unmarshaller.unmarshal(new ByteArrayInputStream(xml));
    }

    private static byte[] marshal(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura faktura) throws Exception {
        Marshaller marshaller =
                JAXBContext.newInstance(io.github.mgrtomaszzurawski.ksef.xml.fa3.Faktura.class).createMarshaller();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(faktura, out);
        return out.toByteArray();
    }
}
