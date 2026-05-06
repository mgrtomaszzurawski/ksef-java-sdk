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

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates valid invoice XML for each {@link FormCode} variant the SDK
 * supports. Templates live as classpath resources under
 * {@code /invoice-templates/} and are filled with caller-supplied seller NIP
 * plus current date / timestamp / unique invoice-number suffix.
 *
 * <p>Variants:
 * <ul>
 *   <li>{@link FormCode#FA2} — KSeF FA(2) (legacy, accepted only on TEST env).</li>
 *   <li>{@link FormCode#FA3} — KSeF FA(3) (current, accepted on TEST/DEMO/PROD).</li>
 *   <li>{@link FormCode#PEF3} — UBL/Peppol-based PEF(3) public-procurement invoice.</li>
 *   <li>{@link FormCode#PEF_KOR3} — PEF correction document.</li>
 * </ul>
 *
 * <p>The PEF templates are adapted from the official
 * {@code CIRFMF/ksef-client-java demo-web-app} samples.
 */
public final class TestInvoiceXml {

    private static final String TEMPLATE_DIR = "/invoice-templates/";
    private static final String TEMPLATE_FA2 = TEMPLATE_DIR + "fa2.xml";
    private static final String TEMPLATE_FA3 = TEMPLATE_DIR + "fa3.xml";
    private static final String TEMPLATE_PEF3 = TEMPLATE_DIR + "pef3.xml";
    private static final String TEMPLATE_PEF_KOR3 = TEMPLATE_DIR + "pef_kor3.xml";

    private static final String PH_SUPPLIER_NIP = "#supplier_nip#";
    private static final String PH_BUYER_NIP = "#buyer_nip#";
    private static final String PH_INVOICE_NUMBER = "#invoice_number#";
    private static final String PH_ISSUE_DATE = "#issue_date#";
    private static final String PH_DUE_DATE = "#due_date#";
    private static final String PH_DATETIME = "#datetime#";
    private static final String PH_BUYER_REFERENCE = "#buyer_reference#";
    private static final String PH_IBAN = "#iban#";

    private static final String DEFAULT_BUYER_NIP = "3861610227";
    private static final String DEFAULT_BUYER_REFERENCE = "BR-DEMO";
    private static final String DEFAULT_IBAN = "PL12345678901234567890123456";
    private static final String INVOICE_NUMBER_PREFIX = "SDK-DEMO-";
    private static final String ERR_TEMPLATE_NOT_FOUND = "Invoice template not found on classpath: ";
    private static final String ERR_UNKNOWN_FORM_CODE = "Unknown FormCode for fixture generation: ";

    private TestInvoiceXml() { }

    /**
     * Generate invoice XML for the supplied {@link FormCode}.
     *
     * @param formCode form code to render — must be one of FA2/FA3/PEF3/PEF_KOR3
     * @param sellerNip 10-digit seller NIP
     * @return invoice XML bytes in UTF-8
     */
    public static byte[] generate(FormCode formCode, String sellerNip) {
        String templatePath = templateFor(formCode);
        String template = loadTemplate(templatePath);
        Map<String, String> placeholders = placeholders(sellerNip);
        String filled = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            filled = filled.replace(entry.getKey(), entry.getValue());
        }
        return filled.getBytes(StandardCharsets.UTF_8);
    }

    /** @deprecated kept for callers that have not migrated to {@link #generate(FormCode, String)}. */
    @Deprecated
    public static byte[] generate(String sellerNip) {
        return generate(FormCode.FA3, sellerNip);
    }

    private static String templateFor(FormCode formCode) {
        if (formCode.equals(FormCode.FA2)) {
            return TEMPLATE_FA2;
        }
        if (formCode.equals(FormCode.FA3)) {
            return TEMPLATE_FA3;
        }
        if (formCode.equals(FormCode.PEF3)) {
            return TEMPLATE_PEF3;
        }
        if (formCode.equals(FormCode.PEF_KOR3)) {
            return TEMPLATE_PEF_KOR3;
        }
        throw new IllegalArgumentException(ERR_UNKNOWN_FORM_CODE + formCode);
    }

    private static Map<String, String> placeholders(String sellerNip) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dueDate = LocalDate.now().plusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String datetime = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String invoiceNumber = INVOICE_NUMBER_PREFIX + System.nanoTime();
        return Map.of(
                PH_SUPPLIER_NIP, sellerNip,
                PH_BUYER_NIP, DEFAULT_BUYER_NIP,
                PH_INVOICE_NUMBER, invoiceNumber,
                PH_ISSUE_DATE, today,
                PH_DUE_DATE, dueDate,
                PH_DATETIME, datetime,
                PH_BUYER_REFERENCE, DEFAULT_BUYER_REFERENCE,
                PH_IBAN, DEFAULT_IBAN
        );
    }

    private static String loadTemplate(String resourcePath) {
        try (InputStream stream = TestInvoiceXml.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException(ERR_TEMPLATE_NOT_FOUND + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioFailure) {
            throw new UncheckedIOException(ERR_TEMPLATE_NOT_FOUND + resourcePath, ioFailure);
        }
    }
}
