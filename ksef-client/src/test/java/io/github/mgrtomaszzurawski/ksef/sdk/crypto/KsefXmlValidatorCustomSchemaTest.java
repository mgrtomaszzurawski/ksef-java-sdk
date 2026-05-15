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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify the F-4 schema-injection contract: a custom {@link FormCode}
 * with an attached XSD validates client-side through the same
 * {@link KsefXmlValidator} pipeline as the bundled predefined forms.
 */
class KsefXmlValidatorCustomSchemaTest {

    private static final String CUSTOM_SYSTEM_CODE = "TEST_CUSTOM (1)";
    private static final String CUSTOM_SCHEMA_VERSION = "1-0E";
    private static final String CUSTOM_VALUE = "TEST_CUSTOM";

    private static final String SIMPLE_XSD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
                       elementFormDefault="qualified">
                <xs:element name="root">
                    <xs:complexType>
                        <xs:sequence>
                            <xs:element name="name" type="xs:string"/>
                            <xs:element name="value" type="xs:int"/>
                        </xs:sequence>
                    </xs:complexType>
                </xs:element>
            </xs:schema>
            """;

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root><name>hello</name><value>42</value></root>
            """;

    private static final String INVALID_XML_MISSING_FIELD = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root><name>hello</name></root>
            """;

    private static final String INVALID_XML_BAD_TYPE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root><name>hello</name><value>not-a-number</value></root>
            """;

    private static FormCode customFormWithSchema() {
        return FormCode.custom(CUSTOM_SYSTEM_CODE, CUSTOM_SCHEMA_VERSION, CUSTOM_VALUE,
                SIMPLE_XSD.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validate_whenCustomFormCodeWithXsdAndValidXml_returnsNoIssues() {
        FormCode form = customFormWithSchema();

        List<KsefXmlValidator.ValidationIssue> issues =
                KsefXmlValidator.validate(VALID_XML.getBytes(StandardCharsets.UTF_8), form);

        assertEquals(0, issues.size(), "valid XML must produce zero issues");
    }

    @Test
    void validate_whenCustomFormCodeWithXsdAndMissingRequiredField_returnsError() {
        FormCode form = customFormWithSchema();

        List<KsefXmlValidator.ValidationIssue> issues = KsefXmlValidator.validate(
                INVALID_XML_MISSING_FIELD.getBytes(StandardCharsets.UTF_8), form);

        assertTrue(issues.stream().anyMatch(i -> i.severity() == KsefXmlValidator.Severity.ERROR
                        || i.severity() == KsefXmlValidator.Severity.FATAL),
                "missing required element must surface as ERROR/FATAL; got: " + issues);
    }

    @Test
    void validate_whenCustomFormCodeWithXsdAndBadType_returnsError() {
        FormCode form = customFormWithSchema();

        List<KsefXmlValidator.ValidationIssue> issues = KsefXmlValidator.validate(
                INVALID_XML_BAD_TYPE.getBytes(StandardCharsets.UTF_8), form);

        assertTrue(issues.stream().anyMatch(i -> i.severity() == KsefXmlValidator.Severity.ERROR
                        || i.severity() == KsefXmlValidator.Severity.FATAL),
                "bad-type element must surface as ERROR/FATAL; got: " + issues);
    }

    @Test
    void validate_whenCustomFormCodeWithoutXsd_throwsBecauseNoBundledXsdMatches() {
        FormCode form = FormCode.custom(CUSTOM_SYSTEM_CODE, CUSTOM_SCHEMA_VERSION, CUSTOM_VALUE);

        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validate(VALID_XML.getBytes(StandardCharsets.UTF_8), form));

        assertTrue(ex.getMessage().contains(CUSTOM_SYSTEM_CODE),
                "diagnostic must name the missing systemCode; got: " + ex.getMessage());
    }

    @Test
    void validate_whenCustomFormCodeWithMalformedXsd_throwsClearError() {
        byte[] malformed = "this is not xsd".getBytes(StandardCharsets.UTF_8);
        FormCode form = FormCode.custom(CUSTOM_SYSTEM_CODE, CUSTOM_SCHEMA_VERSION, CUSTOM_VALUE, malformed);

        KsefXmlValidator.KsefXmlValidationException ex = assertThrows(
                KsefXmlValidator.KsefXmlValidationException.class,
                () -> KsefXmlValidator.validate(VALID_XML.getBytes(StandardCharsets.UTF_8), form));

        assertTrue(ex.getMessage().contains("custom XSD failed to parse"),
                "diagnostic must mention XSD parse failure; got: " + ex.getMessage());
    }
}
