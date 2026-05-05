/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KsefXmlValidatorTest {

    @Test
    void validate_obviouslyMalformedXml_returnsFatalIssue() {
        byte[] notXml = "<<<not even xml".getBytes(StandardCharsets.UTF_8);
        List<String> issues = KsefXmlValidator.validate(notXml, FormCode.FA3);
        assertFalse(issues.isEmpty(),
                "malformed XML must produce at least one issue");
        assertTrue(issues.stream().anyMatch(issue -> issue.startsWith("FATAL")),
                "malformed XML should surface as FATAL: " + issues);
    }

    @Test
    void validateOrThrow_invalidXml_throwsWithIssuesList() {
        // XSD with a wrong root element in the same target namespace
        // produces a SAX cvc-elt error.
        byte[] notInvoice =
                ("<Faktura xmlns=\"http://crd.gov.pl/wzor/2023/06/29/12648/\">"
                        + "<DefinitelyNotValid/></Faktura>")
                        .getBytes(StandardCharsets.UTF_8);
        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validateOrThrow(notInvoice, FormCode.FA3));
        assertFalse(ex.issues().isEmpty(),
                "exception must carry the list of issues for diagnostic display");
    }

    @Test
    void validate_unsupportedFormCode_throwsValidationException() {
        FormCode unsupported = FormCode.custom("XX (1)", "0-0X", "XX");
        byte[] anyXml = "<a/>".getBytes(StandardCharsets.UTF_8);
        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validate(anyXml, unsupported));
        assertTrue(ex.getMessage().contains("no bundled XSD"),
                "diagnostic should explain unsupported form code: " + ex.getMessage());
    }
}
