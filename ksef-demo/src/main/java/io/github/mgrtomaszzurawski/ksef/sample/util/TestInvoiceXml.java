/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Generates minimal valid FA(3) invoice XML accepted by KSeF DEMO/PROD.
 *
 * <p>Adapted from the official {@code CIRFMF/ksef-client-java}
 * {@code demo-web-app} sample template
 * ({@code resources/xml/invoices/sample/invoice-template_v3.xml}). DEMO
 * and PROD reject FA(2); demo runners therefore open sessions with
 * {@code FormCode.FA3} and feed the bytes produced here to
 * {@code session.send(...)}.
 */
public final class TestInvoiceXml {

    private static final String FA3_NAMESPACE = "http://crd.gov.pl/wzor/2025/06/25/13775/";
    private static final String ETD_NAMESPACE = "http://crd.gov.pl/xml/schematy/dziedzinowe/mf/2022/01/05/eD/DefinicjeTypy/";
    private static final String SCHEMA_VERSION = "1-0E";
    private static final String SYSTEM_CODE = "FA (3)";
    private static final String FORM_CODE_VALUE = "FA";
    private static final String INVOICE_NUMBER_PREFIX = "SDK-DEMO-";
    private static final String BUYER_NIP = "3861610227";
    private static final String BUYER_NAME = "SDK Demo Buyer";
    private static final String SELLER_NAME = "SDK Demo Seller";
    private static final String ITEM_NAME = "SDK Demo Service";
    private static final String CURRENCY = "PLN";
    private static final String UNIT = "szt.";
    private static final String VAT_RATE = "23";
    private static final String NET_AMOUNT = "100.00";
    private static final String VAT_AMOUNT = "23.00";
    private static final String GROSS_AMOUNT = "123.00";
    private static final String QUANTITY = "1";
    private static final String NET_UNIT_PRICE = "100.00";

    private TestInvoiceXml() { }

    /**
     * Generate a minimal valid FA(3) invoice XML.
     *
     * @param sellerNip the seller's NIP (10 digits)
     * @return invoice XML bytes in UTF-8
     */
    public static byte[] generate(String sellerNip) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String generationTimestamp = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String invoiceNumber = INVOICE_NUMBER_PREFIX + System.currentTimeMillis();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Faktura xmlns="%s" xmlns:etd="%s">
                  <Naglowek>
                    <KodFormularza kodSystemowy="%s" wersjaSchemy="%s">%s</KodFormularza>
                    <WariantFormularza>3</WariantFormularza>
                    <DataWytworzeniaFa>%s</DataWytworzeniaFa>
                    <SystemInfo>KSeF Java SDK Demo</SystemInfo>
                  </Naglowek>
                  <Podmiot1>
                    <DaneIdentyfikacyjne>
                      <NIP>%s</NIP>
                      <Nazwa>%s</Nazwa>
                    </DaneIdentyfikacyjne>
                    <Adres>
                      <KodKraju>PL</KodKraju>
                      <AdresL1>ul. Testowa 1</AdresL1>
                      <AdresL2>00-001 Warszawa</AdresL2>
                    </Adres>
                  </Podmiot1>
                  <Podmiot2>
                    <DaneIdentyfikacyjne>
                      <NIP>%s</NIP>
                      <Nazwa>%s</Nazwa>
                    </DaneIdentyfikacyjne>
                    <Adres>
                      <KodKraju>PL</KodKraju>
                      <AdresL1>ul. Kupiecka 2</AdresL1>
                      <AdresL2>00-002 Warszawa</AdresL2>
                    </Adres>
                    <DaneKontaktowe>
                      <Email>buyer@example.com</Email>
                    </DaneKontaktowe>
                    <JST>2</JST>
                    <GV>2</GV>
                  </Podmiot2>
                  <Fa>
                    <KodWaluty>%s</KodWaluty>
                    <P_1>%s</P_1>
                    <P_2>FA/%s</P_2>
                    <P_6>%s</P_6>
                    <P_13_1>%s</P_13_1>
                    <P_14_1>%s</P_14_1>
                    <P_15>%s</P_15>
                    <Adnotacje>
                      <P_16>2</P_16>
                      <P_17>2</P_17>
                      <P_18>2</P_18>
                      <P_18A>2</P_18A>
                      <Zwolnienie>
                        <P_19N>1</P_19N>
                      </Zwolnienie>
                      <NoweSrodkiTransportu>
                        <P_22N>1</P_22N>
                      </NoweSrodkiTransportu>
                      <P_23>2</P_23>
                      <PMarzy>
                        <P_PMarzyN>1</P_PMarzyN>
                      </PMarzy>
                    </Adnotacje>
                    <RodzajFaktury>VAT</RodzajFaktury>
                    <FaWiersz>
                      <NrWierszaFa>1</NrWierszaFa>
                      <P_7>%s</P_7>
                      <P_8A>%s</P_8A>
                      <P_8B>%s</P_8B>
                      <P_9A>%s</P_9A>
                      <P_11>%s</P_11>
                      <P_12>%s</P_12>
                    </FaWiersz>
                  </Fa>
                </Faktura>
                """.formatted(
                FA3_NAMESPACE, ETD_NAMESPACE,
                SYSTEM_CODE, SCHEMA_VERSION, FORM_CODE_VALUE,
                generationTimestamp,
                sellerNip, SELLER_NAME,
                BUYER_NIP, BUYER_NAME,
                CURRENCY,
                today, invoiceNumber, today,
                NET_AMOUNT, VAT_AMOUNT, GROSS_AMOUNT,
                ITEM_NAME, UNIT, QUANTITY, NET_UNIT_PRICE, NET_AMOUNT, VAT_RATE
        );

        return xml.getBytes(StandardCharsets.UTF_8);
    }
}
