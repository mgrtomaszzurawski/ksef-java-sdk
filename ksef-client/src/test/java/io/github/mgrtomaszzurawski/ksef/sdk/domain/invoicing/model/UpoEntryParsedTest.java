/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import io.github.mgrtomaszzurawski.ksef.xml.upo.Potwierdzenie;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpoEntryParsedTest {

    private static final String REF = "20260509-SE-1111111111-AABBCC-99";
    private static final String SESSION_REF = "20260509-SE-1111111111-AABBCC-00";
    private static final String KSEF_NUMBER = "1111111111-20260509-9F86D081884C-CC";
    private static final String RECEIVING = "Ministerstwo Finansów";

    @Test
    void parsed_returnsEmpty_whenBytesAreGarbage() {
        UpoEntry entry = new UpoEntry(REF, "<not-a-upo/>".getBytes(StandardCharsets.UTF_8));
        assertTrue(entry.parsed().isEmpty());
    }

    @Test
    void parsed_returnsEmpty_whenBytesAreEmpty() {
        UpoEntry entry = new UpoEntry(REF, new byte[0]);
        assertTrue(entry.parsed().isEmpty());
    }

    @Test
    void parsed_returnsSummary_whenBytesAreValidUpo() throws Exception {
        UpoEntry entry = new UpoEntry(REF, minimalUpoXml());
        Optional<UpoSummary> summary = entry.parsed();
        assertTrue(summary.isPresent());
        assertEquals(SESSION_REF, summary.get().upoReferenceNumber());
        assertEquals(1, summary.get().ksefNumbers().size());
        assertEquals(KSEF_NUMBER, summary.get().ksefNumbers().get(0).asString());
    }

    @Test
    void xmlBytes_returnsDefensiveCopy_afterAccessor() {
        byte[] original = "abc".getBytes(StandardCharsets.UTF_8);
        UpoEntry entry = new UpoEntry(REF, original);
        byte[] first = entry.xmlBytes();
        first[0] = 'Z';
        assertFalse(first[0] == entry.xmlBytes()[0],
                "mutating returned array must not affect entry state");
    }

    private static byte[] minimalUpoXml() throws Exception {
        XMLGregorianCalendar issuedAt = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(new GregorianCalendar(TimeZone.getTimeZone("UTC")));

        Potwierdzenie.Dokument document = new Potwierdzenie.Dokument();
        document.setNumerKSeFDokumentu(KSEF_NUMBER);
        document.setDataNadaniaNumeruKSeF(issuedAt);

        Potwierdzenie potwierdzenie = new Potwierdzenie();
        potwierdzenie.setNumerReferencyjnySesji(SESSION_REF);
        potwierdzenie.setNazwaPodmiotuPrzyjmujacego(RECEIVING);
        potwierdzenie.getDokument().add(document);

        JAXBContext context = JAXBContext.newInstance(Potwierdzenie.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        marshaller.marshal(potwierdzenie, output);
        return output.toByteArray();
    }
}
