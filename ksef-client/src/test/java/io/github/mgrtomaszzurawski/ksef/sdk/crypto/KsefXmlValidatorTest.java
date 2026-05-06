/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsefXmlValidatorTest {

    @Test
    void validate_obviouslyMalformedXml_returnsFatalIssue() {
        // given
        byte[] notXml = "<<<not even xml".getBytes(StandardCharsets.UTF_8);

        // when
        List<String> issues = KsefXmlValidator.validate(notXml, FormCode.FA3);

        // then
        assertFalse(issues.isEmpty(),
                "malformed XML must produce at least one issue");
        assertTrue(issues.stream().anyMatch(issue -> issue.startsWith("FATAL")),
                "malformed XML should surface as FATAL: " + issues);
    }

    @Test
    void validateOrThrow_invalidXml_throwsWithIssuesList() {
        // given — XSD with a wrong root element in the same target namespace
        // produces a SAX cvc-elt error.
        byte[] notInvoice =
                ("<Faktura xmlns=\"http://crd.gov.pl/wzor/2023/06/29/12648/\">"
                        + "<DefinitelyNotValid/></Faktura>")
                        .getBytes(StandardCharsets.UTF_8);

        // when / then
        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validateOrThrow(notInvoice, FormCode.FA3));
        assertFalse(ex.issues().isEmpty(),
                "exception must carry the list of issues for diagnostic display");
    }

    @Test
    void validate_unsupportedFormCode_throwsValidationException() {
        // given
        FormCode unsupported = FormCode.custom("XX (1)", "0-0X", "XX");
        byte[] anyXml = "<a/>".getBytes(StandardCharsets.UTF_8);

        // when / then
        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validate(anyXml, unsupported));
        assertTrue(ex.getMessage().contains("no bundled XSD"),
                "diagnostic should explain unsupported form code: " + ex.getMessage());
    }

    @Test
    void validateDetailed_malformedXml_returnsFatalIssueWithSeverity() {
        // given
        byte[] notXml = "<<<not even xml".getBytes(StandardCharsets.UTF_8);

        // when
        List<KsefXmlValidator.ValidationIssue> issues =
                KsefXmlValidator.validateDetailed(notXml, FormCode.FA3);

        // then
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> i.severity() == KsefXmlValidator.Severity.FATAL));
    }

    @Test
    void validationIssue_toString_includesSeverityAndMessage() {
        // given
        KsefXmlValidator.ValidationIssue issue = new KsefXmlValidator.ValidationIssue(
                KsefXmlValidator.Severity.ERROR, 1, 2, "boom");

        // when
        String text = issue.toString();

        // then
        assertTrue(text.contains("ERROR"));
        assertTrue(text.contains("boom"));
    }

    @Test
    void checkRecommendedCharsets_validXml_returnsEmptyList() {
        // given
        byte[] xml = "<a>hello world</a>".getBytes(StandardCharsets.UTF_8);

        // when
        List<String> findings = KsefXmlValidator.checkRecommendedCharsets(xml);

        // then
        assertTrue(findings.isEmpty());
    }

    @Test
    void checkRecommendedCharsets_acceptsTabLfCr() {
        // given
        byte[] xml = "<a>\thello\nworld\r</a>".getBytes(StandardCharsets.UTF_8);

        // when
        List<String> findings = KsefXmlValidator.checkRecommendedCharsets(xml);

        // then
        assertTrue(findings.isEmpty());
    }

    @Test
    void checkRecommendedCharsets_bannedControlChar_returnsFinding() {
        // given
        byte[] xml = ("<a>" + (char) 0x01 + "</a>").getBytes(StandardCharsets.UTF_8);

        // when
        List<String> findings = KsefXmlValidator.checkRecommendedCharsets(xml);

        // then
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("0001"));
    }

    @Test
    void checkRecommendedCharsets_bannedFffe_returnsFinding() {
        // given
        byte[] xml = ("<a>" + (char) 0xFFFE + "</a>").getBytes(StandardCharsets.UTF_8);

        // when
        List<String> findings = KsefXmlValidator.checkRecommendedCharsets(xml);

        // then
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("FFFE"));
    }

    @Test
    void checkRecommendedCharsets_nullInput_throwsNpe() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> KsefXmlValidator.checkRecommendedCharsets(null));
    }
}
