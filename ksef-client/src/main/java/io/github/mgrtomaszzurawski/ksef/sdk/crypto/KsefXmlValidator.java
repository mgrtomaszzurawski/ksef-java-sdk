/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.crypto;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 * Client-side XSD validator for KSeF invoice XML payloads.
 *
 * <p>Each {@link FormCode} maps to one of the bundled XSD schemas
 * shipped under {@code ksef-client/xsd/}. The validator parses and
 * caches each schema per JVM the first time it is requested; subsequent
 * validation calls reuse the parsed {@link Schema} instance.
 *
 * <p>This is a pre-flight check — it catches structural / namespace /
 * required-field mistakes before the SDK encrypts and posts the
 * invoice. The server still validates server-side; passing the local
 * validator does not guarantee server acceptance (KSeF business rules
 * are not in the XSD).
 *
 * <p>The validator throws {@link KsefXmlValidationException} (a
 * {@link KsefException} subclass) on failure with the SAX line/column
 * info carried in the message. {@link #validate(byte[], FormCode)}
 * returns the list of validation issues — empty when the document is
 * valid.
 *
 * @since 1.0.0
 */
public final class KsefXmlValidator {

    private static final String ERR_NULL_XML = "invoiceXml must not be null";
    private static final String ERR_NULL_FORM = "formCode must not be null";
    private static final String ERR_SCHEMA_LOAD = "Failed to load XSD schema for form code: ";
    private static final String ERR_VALIDATION_FAILED = "Invoice XML failed XSD validation: ";
    private static final String ISSUE_SEPARATOR = " — ";
    private static final String LINE_LABEL = " line ";
    private static final String COLUMN_LABEL = ":";

    /** Resource path inside the SDK jar for each {@link FormCode#systemCode()}. */
    private static final java.util.Map<String, String> SCHEMA_RESOURCE_BY_SYSTEM_CODE = java.util.Map.of(
            "FA (2)", "/xsd/FA/schemat_FA(2)_v1-0E.xsd",
            "FA (3)", "/xsd/FA/schemat_FA(3)_v1-0E.xsd"
    );

    private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    private KsefXmlValidator() { }

    /**
     * Validate {@code invoiceXml} against the XSD shipped for the
     * supplied {@link FormCode}. Returns the (possibly empty) list of
     * SAX validation issues — empty list means valid.
     *
     * @throws KsefXmlValidationException when the form code has no
     *     bundled XSD or the XSD itself fails to load.
     */
    public static List<String> validate(byte[] invoiceXml, FormCode formCode) {
        Objects.requireNonNull(invoiceXml, ERR_NULL_XML);
        Objects.requireNonNull(formCode, ERR_NULL_FORM);
        Schema schema = SCHEMA_CACHE.computeIfAbsent(formCode.systemCode(),
                KsefXmlValidator::loadSchemaOrThrow);
        java.util.List<String> issues = new java.util.ArrayList<>();
        Validator validator = schema.newValidator();
        try {
            // XXE hardening on the validator itself — the schema is already
            // hardened, but each Validator instance also accepts these props.
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException ignored) {
            // properties unsupported on some JAXP impls
        }
        validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void warning(org.xml.sax.SAXParseException ex) {
                issues.add(formatIssue("WARNING", ex));
            }
            @Override
            public void error(org.xml.sax.SAXParseException ex) {
                issues.add(formatIssue("ERROR", ex));
            }
            @Override
            public void fatalError(org.xml.sax.SAXParseException ex) {
                issues.add(formatIssue("FATAL", ex));
            }
        });
        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(invoiceXml)));
        } catch (SAXException | IOException ex) {
            issues.add("FATAL" + ISSUE_SEPARATOR + ex.getMessage());
        }
        return List.copyOf(issues);
    }

    /**
     * Validate-or-throw shorthand. Throws {@link KsefXmlValidationException}
     * with all collected issues when {@code invoiceXml} fails validation.
     */
    public static void validateOrThrow(byte[] invoiceXml, FormCode formCode) {
        List<String> issues = validate(invoiceXml, formCode);
        if (!issues.isEmpty()) {
            throw new KsefXmlValidationException(ERR_VALIDATION_FAILED + String.join("; ", issues), issues);
        }
    }

    private static String formatIssue(String severity, org.xml.sax.SAXParseException ex) {
        return severity + LINE_LABEL + ex.getLineNumber() + COLUMN_LABEL + ex.getColumnNumber()
                + ISSUE_SEPARATOR + ex.getMessage();
    }

    private static Schema loadSchemaOrThrow(String systemCode) {
        String resource = SCHEMA_RESOURCE_BY_SYSTEM_CODE.get(systemCode);
        if (resource == null) {
            throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode + " (no bundled XSD; supported: "
                    + SCHEMA_RESOURCE_BY_SYSTEM_CODE.keySet() + ")", List.of());
        }
        try (InputStream stream = KsefXmlValidator.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode
                        + " (resource not found on classpath: " + resource + ")", List.of());
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            // XXE hardening: explicitly forbid the parser from resolving DTD
            // declarations or external entities. The classpath resolver below
            // is the only sanctioned source of external XSD references.
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            // FA(3) schema is dense enough to trip the JDK default 5_000-node
            // expansion ceiling. Raising it just for this factory is safe — the
            // parser does not visit external (network) entities thanks to the
            // ClasspathResourceResolver below; the only schemas it sees are
            // the bundled ones.
            try {
                factory.setProperty("http://www.oracle.com/xml/jaxp/properties/maxOccurLimit", 0);
            } catch (SAXException ignored) {
                // Property unsupported in some JAXP impls — schema may still load.
            }
            // KSeF FA(2)/(3) XSDs reference bundled "bazowe/*" via http:// URLs.
            // Resolve those locally from /xsd/FA/bazowe/ on the classpath so the
            // SchemaFactory does not attempt network access (the
            // FEATURE_SECURE_PROCESSING above otherwise blocks it with
            // "http access is not allowed").
            factory.setResourceResolver(new ClasspathResourceResolver());
            return factory.newSchema(new StreamSource(stream));
        } catch (SAXException | IOException ex) {
            throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode + " — " + ex.getMessage(), List.of());
        }
    }

    private static final class ClasspathResourceResolver implements org.w3c.dom.ls.LSResourceResolver {

        private static final String BAZOWE_RESOURCE_PREFIX = "/xsd/FA/bazowe/";

        @Override
        @SuppressWarnings("PMD.UseObjectForClearerAPI") // signature fixed by W3C LSResourceResolver interface
        public org.w3c.dom.ls.LSInput resolveResource(String type, String namespaceURI, String publicId,
                                                       String systemId, String baseURI) {
            if (systemId == null) {
                return null;
            }
            // Map http://...something.xsd → /xsd/FA/bazowe/something.xsd on the classpath.
            int lastSlash = systemId.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? systemId.substring(lastSlash + 1) : systemId;
            InputStream resourceStream = KsefXmlValidator.class.getResourceAsStream(
                    BAZOWE_RESOURCE_PREFIX + fileName);
            if (resourceStream == null) {
                return null;
            }
            return new ClasspathLsInput(systemId, publicId, baseURI, resourceStream);
        }
    }

    private static final class ClasspathLsInput implements org.w3c.dom.ls.LSInput {

        private final String systemId;
        private final String publicId;
        private final String baseURI;
        private final InputStream byteStream;

        ClasspathLsInput(String systemId, String publicId, String baseURI, InputStream byteStream) {
            this.systemId = systemId;
            this.publicId = publicId;
            this.baseURI = baseURI;
            this.byteStream = byteStream;
        }

        @Override public java.io.Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(java.io.Reader characterStream) { /* unused */ }
        @Override public InputStream getByteStream() { return byteStream; }
        @Override public void setByteStream(InputStream stream) { /* unused */ }
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String stringData) { /* unused */ }
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String value) { /* unused */ }
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String value) { /* unused */ }
        @Override public String getBaseURI() { return baseURI; }
        @Override public void setBaseURI(String value) { /* unused */ }
        @Override public String getEncoding() { return null; }
        @Override public void setEncoding(String value) { /* unused */ }
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean value) { /* unused */ }
    }

    /**
     * Thrown by {@link #validateOrThrow} when {@code invoiceXml} fails
     * XSD validation, or by {@link #validate} when the requested
     * {@link FormCode} has no bundled XSD.
     *
     * @since 1.0.0
     */
    public static class KsefXmlValidationException extends KsefException {

        private static final long serialVersionUID = 1L;

        private final List<String> issues;

        public KsefXmlValidationException(String message, List<String> issues) {
            super(message, null);
            this.issues = List.copyOf(issues);
        }

        public List<String> issues() {
            return issues;
        }
    }
}
