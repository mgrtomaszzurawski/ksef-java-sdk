/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String PH_KSEF_NUMBER = "#ksef_number#";

    private static final String DEFAULT_BUYER_NIP = "3861610227";
    private static final String DEFAULT_BUYER_REFERENCE = "BR-DEMO";
    private static final String DEFAULT_IBAN = "PL11111111111111111111123456";
    private static final String INVOICE_NUMBER_PREFIX = "SDK-DEMO-";
    private static final int DEFAULT_PAYMENT_TERM_DAYS = 14;
    private static final String ERR_TEMPLATE_NOT_FOUND = "Invoice template not found on classpath: ";
    private static final String ERR_UNKNOWN_FORM_CODE = "Unknown FormCode for fixture generation: ";
    private static final String ERR_PEF_KOR_REQUIRES_KSEF_NUMBER =
            "PEF_KOR(3) is a correction document — caller must supply the KSeF number "
                    + "of the prior invoice via generate(formCode, sellerNip, priorKsefNumber).";
    private static final String ERR_UNRESOLVED_PLACEHOLDER =
            "Invoice fixture still contains unresolved SDK placeholder(s) after substitution: ";
    /**
     * Pattern matches only the SDK's own placeholder names, not arbitrary
     * {@code #foo#} substrings. PEPPOL CustomizationID URNs legitimately
     * contain fragments like {@code #compliant#} / {@code #extended#}
     * which must NOT be flagged as unresolved placeholders.
     */
    private static final Pattern UNRESOLVED_PLACEHOLDER_PATTERN = Pattern.compile(
            "#(supplier_nip|buyer_nip|invoice_number|issue_date|due_date|datetime|buyer_reference|iban|ksef_number)#");

    private TestInvoiceXml() { }

    /**
     * Generate invoice XML for the supplied {@link FormCode}. PEF_KOR(3)
     * fixtures need a prior KSeF number — use
     * {@link #generate(FormCode, String, String)} for that variant.
     *
     * @param formCode form code to render — must be one of FA2/FA3/PEF3
     * @param sellerNip 10-digit seller NIP
     * @return invoice XML bytes in UTF-8
     */
    public static byte[] generate(FormCode formCode, String sellerNip) {
        if (formCode.equals(FormCode.PEF_KOR3)) {
            throw new IllegalArgumentException(ERR_PEF_KOR_REQUIRES_KSEF_NUMBER);
        }
        return generate(formCode, sellerNip, null);
    }

    /**
     * Generate invoice XML, supplying the KSeF number of a prior invoice
     * (required for PEF_KOR(3) corrections). For non-correction form
     * codes the {@code priorKsefNumber} is ignored.
     *
     * @param formCode form code to render
     * @param sellerNip 10-digit seller NIP
     * @param priorKsefNumber KSeF number of the prior invoice this
     *     correction references — required for PEF_KOR3, ignored otherwise
     * @return invoice XML bytes in UTF-8
     */
    public static byte[] generate(FormCode formCode, String sellerNip, String priorKsefNumber) {
        if (formCode.equals(FormCode.PEF_KOR3) && (priorKsefNumber == null || priorKsefNumber.isBlank())) {
            throw new IllegalArgumentException(ERR_PEF_KOR_REQUIRES_KSEF_NUMBER);
        }
        String templatePath = templateFor(formCode);
        String template = loadTemplate(templatePath);
        Map<String, String> placeholders = placeholders(sellerNip, priorKsefNumber);
        String filled = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            filled = filled.replace(entry.getKey(), entry.getValue());
        }
        assertNoUnresolvedPlaceholders(filled, formCode);
        return filled.getBytes(StandardCharsets.UTF_8);
    }

    /** @deprecated kept for callers that have not migrated to {@link #generate(FormCode, String)}. */
    @Deprecated
    public static byte[] generate(String sellerNip) {
        return generate(FormCode.FA3, sellerNip);
    }

    private static void assertNoUnresolvedPlaceholders(String filled, FormCode formCode) {
        Matcher matcher = UNRESOLVED_PLACEHOLDER_PATTERN.matcher(filled);
        if (matcher.find()) {
            throw new IllegalStateException(ERR_UNRESOLVED_PLACEHOLDER + matcher.group()
                    + " (formCode=" + formCode + ")");
        }
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

    private static Map<String, String> placeholders(String sellerNip, String priorKsefNumber) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String dueDate = LocalDate.now().plusDays(DEFAULT_PAYMENT_TERM_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String datetime = OffsetDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        String invoiceNumber = INVOICE_NUMBER_PREFIX + System.nanoTime();
        Map<String, String> map = new HashMap<>();
        map.put(PH_SUPPLIER_NIP, sellerNip);
        map.put(PH_BUYER_NIP, DEFAULT_BUYER_NIP);
        map.put(PH_INVOICE_NUMBER, invoiceNumber);
        map.put(PH_ISSUE_DATE, today);
        map.put(PH_DUE_DATE, dueDate);
        map.put(PH_DATETIME, datetime);
        map.put(PH_BUYER_REFERENCE, DEFAULT_BUYER_REFERENCE);
        map.put(PH_IBAN, DEFAULT_IBAN);
        if (priorKsefNumber != null) {
            map.put(PH_KSEF_NUMBER, priorKsefNumber);
        }
        return map;
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
