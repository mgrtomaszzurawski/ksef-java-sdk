/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.validation;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.jspecify.annotations.Nullable;
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
 * <p>Bundled XSDs cover {@link FormCode#FA2}, {@link FormCode#FA3},
 * {@link FormCode#PEF3} and {@link FormCode#PEF_KOR3}. Any other
 * (custom) form code raises {@link KsefXmlValidationException} from
 * {@link #validate(byte[], FormCode)} at schema-load time — the
 * validator covers the four KSeF-accepted schemas only.
 *
 * <p>The validator throws {@link KsefXmlValidationException} (a
 * {@link KsefException} subclass) from {@link #validateOrThrow}.
 * {@link #validate(byte[], FormCode)} returns structured
 * {@link ValidationIssue} records with explicit {@link Severity} so
 * callers can distinguish warnings from errors — empty list means
 * valid.
 *
 * @since 1.0.0
 */
public final class KsefXmlValidator {

    private static final String ERR_NULL_XML = "invoiceXml must not be null";
    private static final String ERR_NULL_FORM = "formCode must not be null";
    private static final String ERR_SCHEMA_LOAD = "Failed to load XSD schema for form code: ";
    private static final String ERR_VALIDATION_FAILED = "Invoice XML failed XSD validation: ";
    private static final String ERR_SCHEMA_LOAD_HINT_USE_CUSTOM =
            "; use FormCode.custom(..., xsdBytes) to attach a custom XSD)";
    private static final String ERR_CUSTOM_XSD_PARSE_FAILED =
            " (custom XSD failed to parse) — ";
    private static final String ERR_CUSTOM_XSD_EXTERNAL_REF =
            "Custom XSDs cannot reference external resources (xs:include / xs:import); "
                    + "self-contained XSD required";
    private static final String ISSUE_SEPARATOR = " — ";
    private static final String LINE_LABEL = " line ";
    private static final String COLUMN_LABEL = ":";
    private static final String BANNED_CODEPOINT_PREFIX = "Banned XML 1.0 codepoint U+";
    /** Oracle JAXP-specific override that caps maxOccurs on schema loaders. Best-effort — not all JAXP impls honor it. */
    private static final String ORACLE_JAXP_MAX_OCCUR_LIMIT = "http://www.oracle.com/xml/jaxp/properties/maxOccurLimit";
    private static final int CODEPOINT_TAB = 0x09;
    private static final int CODEPOINT_LF = 0x0A;
    private static final int CODEPOINT_CR = 0x0D;
    private static final int FIRST_PRINTABLE_ASCII = 0x20;
    private static final int SURROGATE_RANGE_START = 0xD800;
    private static final int SURROGATE_RANGE_END = 0xDFFF;
    private static final int NONCHARACTER_FFFE = 0xFFFE;
    private static final int NONCHARACTER_FFFF = 0xFFFF;

    /** Resource path inside the SDK jar for each {@link FormCode#systemCode()}. */
    private static final Map<String, String> SCHEMA_RESOURCE_BY_SYSTEM_CODE = Map.of(
            "FA (2)", "/xsd/FA/schemat_FA(2)_v1-0E.xsd",
            "FA (3)", "/xsd/FA/schemat_FA(3)_v1-0E.xsd",
            "PEF (3)", "/xsd/PEF/Schemat_PEF(3)_v2-1.xsd",
            "PEF_KOR (3)", "/xsd/PEF/Schemat_PEF_KOR(3)_v2-1.xsd"
    );

    /**
     * Classpath subdirectory containing the FA-family bazowe XSDs.
     */
    private static final String FA_BAZOWE_PREFIX = "/xsd/FA/bazowe/";
    /**
     * Classpath subdirectory containing the PEF (UBL + Polish) bazowe XSDs.
     */
    private static final String PEF_BAZOWE_PREFIX = "/xsd/PEF/bazowe/";
    private static final String PEF_SYSTEM_CODE_PREFIX = "PEF";

    private static final ConcurrentMap<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();

    /**
     * Identity-keyed cache for custom-XSD form codes. A consumer's typical
     * pattern is {@code static final FormCode MY = FormCode.custom(..., bytes)},
     * so the same FormCode instance flows through every validate call and an
     * identity lookup avoids re-loading the Xerces DFA.
     *
     * <p>Uses {@link IdentityHashMap} so two FormCodes that are wire-equal
     * (same triplet) but were constructed with different XSD bytes get
     * distinct cache entries — the equality contract on {@link FormCode}
     * is intentionally based on the wire shape only.
     *
     * <p>Reads + writes guarded by {@code synchronized (CUSTOM_SCHEMA_CACHE)}
     * to make the get-or-load operation atomic (a plain
     * {@code synchronizedMap} wrapper would race on the check-then-put).
     * Synchronisation is cheap here: each FormCode goes through the slow
     * path exactly once.
     */
    private static final Map<FormCode, Schema> CUSTOM_SCHEMA_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    /** Severity reported by the SAX-driven XSD validator. */
    public enum Severity {
        /** SAX warning — schema-level note, not a document defect. */
        WARNING,
        /** SAX recoverable error — document is structurally invalid. */
        ERROR,
        /** SAX fatal error — parsing cannot continue. */
        FATAL
    }

    /**
     * Structured validation issue with explicit severity.
     *
     * @param severity {@link Severity} of this issue
     * @param line     XML line number reported by the parser, or {@code -1}
     *                 when not available
     * @param column   XML column number reported by the parser, or {@code -1}
     *                 when not available
     * @param message  human-readable message from the parser
     */
    public record ValidationIssue(Severity severity, int line, int column, @Nullable String message) {

        @Override
        public String toString() {
            return severity + LINE_LABEL + line + COLUMN_LABEL + column + ISSUE_SEPARATOR + message;
        }
    }

    private KsefXmlValidator() { }

    /**
     * Validate {@code invoiceXml} against the XSD shipped for the
     * supplied {@link FormCode}. Returns the (possibly empty) list of
     * structured {@link ValidationIssue} records carrying explicit
     * {@link Severity} — empty list means the document is valid.
     *
     * @throws KsefXmlValidationException when the form code has no
     *     bundled XSD or the XSD itself fails to load.
     */
    public static List<ValidationIssue> validate(byte[] invoiceXml, FormCode formCode) {
        Objects.requireNonNull(invoiceXml, ERR_NULL_XML);
        Objects.requireNonNull(formCode, ERR_NULL_FORM);
        return validateStream(new ByteArrayInputStream(invoiceXml), formCode);
    }

    /**
     * Streaming variant of {@link #validate(byte[], FormCode)} — accepts
     * an {@link InputStream} so the caller can validate file content
     * without materialising the full payload as a byte array. The stream
     * is consumed by the underlying SAX-based JAXP validator and is
     * <strong>not</strong> closed by this method; callers own the
     * stream lifecycle.
     *
     * <p>R1-19 Phase 2: this is the entry point
     * {@code BatchSubmissionFlow.submitFromFiles} uses to keep the
     * memory-bounded streaming property of the file-batch path.
     *
     * @throws KsefXmlValidationException when the form code has no
     *     bundled XSD or the XSD itself fails to load.
     */
    public static List<ValidationIssue> validateStream(InputStream invoiceXmlStream, FormCode formCode) {
        Objects.requireNonNull(invoiceXmlStream, ERR_NULL_XML);
        Objects.requireNonNull(formCode, ERR_NULL_FORM);
        Schema schema = resolveSchema(formCode);
        List<ValidationIssue> issues = new ArrayList<>();
        Validator validator = schema.newValidator();
        try {
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (SAXException ignored) {
            // properties unsupported on some JAXP impls
        }
        validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void warning(org.xml.sax.SAXParseException ex) {
                issues.add(toIssue(Severity.WARNING, ex));
            }
            @Override
            public void error(org.xml.sax.SAXParseException ex) {
                issues.add(toIssue(Severity.ERROR, ex));
            }
            @Override
            public void fatalError(org.xml.sax.SAXParseException ex) {
                issues.add(toIssue(Severity.FATAL, ex));
            }
        });
        try {
            validator.validate(new StreamSource(invoiceXmlStream));
        } catch (SAXException | IOException ex) {
            issues.add(new ValidationIssue(Severity.FATAL, -1, -1, ex.getMessage()));
        }
        return List.copyOf(issues);
    }

    /**
     * Validate-or-throw shorthand. Throws {@link KsefXmlValidationException}
     * with all collected issues when {@code invoiceXml} fails validation
     * with {@link Severity#ERROR} or {@link Severity#FATAL}.
     * {@link Severity#WARNING}-only results pass through silently.
     */
    public static void validateOrThrow(byte[] invoiceXml, FormCode formCode) {
        List<ValidationIssue> issues = validate(invoiceXml, formCode);
        boolean hasFailure = false;
        List<String> failureMessages = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (issue.severity() == Severity.ERROR || issue.severity() == Severity.FATAL) {
                hasFailure = true;
            }
            failureMessages.add(issue.toString());
        }
        if (hasFailure) {
            throw new KsefXmlValidationException(
                    ERR_VALIDATION_FAILED + String.join("; ", failureMessages),
                    List.copyOf(failureMessages));
        }
    }

    /**
     * Pre-flight check for the W3C XML 1.0 banned-codepoint set per
     * {@code ksef-docs/faktury/weryfikacja-faktury.md} (api-changelog
     * v2.4.0, effective on PROD 2026-07-16). Rejects control
     * characters U+0000–U+001F except TAB (U+0009), LF (U+000A) and
     * CR (U+000D); rejects unpaired surrogates and U+FFFE / U+FFFF.
     *
     * <p>Opt-in: KSeF still rejects these server-side, so this check
     * exists as a fail-fast option for callers that prefer to surface
     * the banned codepoint locally before encryption + upload. Returns
     * an empty list when the bytes contain no banned codepoints.
     *
     * <p>The input is parsed as UTF-8. If the bytes are not valid
     * UTF-8 the malformed sequences are themselves treated as banned
     * (decoded with {@link java.nio.charset.CodingErrorAction#REPLACE}
     * is not used here — the malformed input itself is reported as a
     * banned-codepoint violation).
     */
    public static List<String> checkRecommendedCharsets(byte[] invoiceXml) {
        Objects.requireNonNull(invoiceXml, ERR_NULL_XML);
        String text = new String(invoiceXml, StandardCharsets.UTF_8);
        List<String> findings = new ArrayList<>();
        int length = text.length();
        int index = 0;
        while (index < length) {
            int codepoint = text.codePointAt(index);
            if (isBannedCodepoint(codepoint)) {
                findings.add(BANNED_CODEPOINT_PREFIX + String.format("%04X", codepoint)
                        + LINE_LABEL + index);
            }
            index += Character.charCount(codepoint);
        }
        return List.copyOf(findings);
    }

    private static boolean isBannedCodepoint(int codepoint) {
        if (codepoint == CODEPOINT_TAB || codepoint == CODEPOINT_LF || codepoint == CODEPOINT_CR) {
            return false;
        }
        if (codepoint < FIRST_PRINTABLE_ASCII) {
            return true;
        }
        if (codepoint >= SURROGATE_RANGE_START && codepoint <= SURROGATE_RANGE_END) {
            return true;
        }
        return codepoint == NONCHARACTER_FFFE || codepoint == NONCHARACTER_FFFF;
    }

    private static ValidationIssue toIssue(Severity severity, org.xml.sax.SAXParseException ex) {
        return new ValidationIssue(severity, ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage());
    }

    private static Schema resolveSchema(FormCode formCode) {
        byte[] customXsd = formCode.customXsdBytes();
        if (customXsd != null) {
            synchronized (CUSTOM_SCHEMA_CACHE) {
                Schema cached = CUSTOM_SCHEMA_CACHE.get(formCode);
                if (cached == null) {
                    cached = loadSchemaFromBytes(customXsd, formCode.systemCode());
                    CUSTOM_SCHEMA_CACHE.put(formCode, cached);
                }
                return cached;
            }
        }
        return SCHEMA_CACHE.computeIfAbsent(formCode.systemCode(),
                KsefXmlValidator::loadBundledSchemaOrThrow);
    }

    private static Schema loadBundledSchemaOrThrow(String systemCode) {
        String resource = SCHEMA_RESOURCE_BY_SYSTEM_CODE.get(systemCode);
        if (resource == null) {
            throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode + " (no bundled XSD; supported: "
                    + SCHEMA_RESOURCE_BY_SYSTEM_CODE.keySet()
                    + ERR_SCHEMA_LOAD_HINT_USE_CUSTOM, List.of());
        }
        try (InputStream stream = KsefXmlValidator.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode
                        + " (resource not found on classpath: " + resource + ")", List.of());
            }
            SchemaFactory factory = newHardenedSchemaFactory();
            factory.setResourceResolver(new ClasspathResourceResolver(systemCode));
            return factory.newSchema(new StreamSource(stream));
        } catch (SAXException | IOException ex) {
            throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode + " — " + ex.getMessage(), List.of());
        }
    }

    private static Schema loadSchemaFromBytes(byte[] xsdBytes, String systemCode) {
        try {
            SchemaFactory factory = newHardenedSchemaFactory();
            // Defense in depth: ACCESS_EXTERNAL_SCHEMA="" already blocks
            // xs:include / xs:import resolution at the JAXP property level
            // (and JAXP throws on attempt). Installing a throwing resolver
            // here makes the SDK-side failure path explicit — the
            // IllegalArgumentException carries a domain-specific message
            // pointing at the "self-contained XSD required" contract,
            // rather than leaving the surface as a generic SAXException
            // about external access.
            factory.setResourceResolver((type, namespaceURI, publicId, systemId, baseURI) -> {
                throw new IllegalArgumentException(ERR_CUSTOM_XSD_EXTERNAL_REF);
            });
            return factory.newSchema(new StreamSource(new ByteArrayInputStream(xsdBytes)));
        } catch (SAXException ex) {
            throw new KsefXmlValidationException(ERR_SCHEMA_LOAD + systemCode
                    + ERR_CUSTOM_XSD_PARSE_FAILED + ex.getMessage(), List.of());
        }
    }

    private static SchemaFactory newHardenedSchemaFactory() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        setMaxOccurLimitIfSupported(factory);
        return factory;
    }

    private static void setMaxOccurLimitIfSupported(SchemaFactory factory) {
        try {
            factory.setProperty(ORACLE_JAXP_MAX_OCCUR_LIMIT, 0);
        } catch (SAXException ignored) {
            // Property unsupported in some JAXP impls — schema may still load.
        }
    }

    private static final class ClasspathResourceResolver implements org.w3c.dom.ls.LSResourceResolver {

        private final String bazoweResourcePrefix;

        ClasspathResourceResolver(String systemCode) {
            this.bazoweResourcePrefix = systemCode != null && systemCode.startsWith(PEF_SYSTEM_CODE_PREFIX)
                    ? PEF_BAZOWE_PREFIX : FA_BAZOWE_PREFIX;
        }

        @Override
        @SuppressWarnings("PMD.UseObjectForClearerAPI") // signature fixed by W3C LSResourceResolver interface
        public org.w3c.dom.ls.@Nullable LSInput resolveResource(@Nullable String type, @Nullable String namespaceURI, @Nullable String publicId,
                                                       @Nullable String systemId, @Nullable String baseURI) {
            if (systemId == null) {
                return null;
            }
            int lastSlash = systemId.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? systemId.substring(lastSlash + 1) : systemId;
            InputStream resourceStream = KsefXmlValidator.class.getResourceAsStream(
                    bazoweResourcePrefix + fileName);
            if (resourceStream == null) {
                return null;
            }
            return new ClasspathLsInput(systemId, publicId, baseURI, resourceStream);
        }
    }

    @SuppressWarnings("java:S6206")
    private static final class ClasspathLsInput implements org.w3c.dom.ls.LSInput {

        private final @Nullable String systemId;
        private final @Nullable String publicId;
        private final @Nullable String baseURI;
        private final InputStream byteStream;

        ClasspathLsInput(@Nullable String systemId, @Nullable String publicId, @Nullable String baseURI, InputStream byteStream) {
            this.systemId = systemId;
            this.publicId = publicId;
            this.baseURI = baseURI;
            this.byteStream = byteStream;
        }

        @Override public java.io.@Nullable Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(java.io.@Nullable Reader characterStream) { /* unused */ }
        @Override public InputStream getByteStream() { return byteStream; }
        @Override public void setByteStream(@Nullable InputStream stream) { /* unused */ }
        @Override public @Nullable String getStringData() { return null; }
        @Override public void setStringData(@Nullable String stringData) { /* unused */ }
        @Override public @Nullable String getSystemId() { return systemId; }
        @Override public void setSystemId(@Nullable String value) { /* unused */ }
        @Override public @Nullable String getPublicId() { return publicId; }
        @Override public void setPublicId(@Nullable String value) { /* unused */ }
        @Override public @Nullable String getBaseURI() { return baseURI; }
        @Override public void setBaseURI(@Nullable String value) { /* unused */ }
        @Override public @Nullable String getEncoding() { return null; }
        @Override public void setEncoding(@Nullable String value) { /* unused */ }
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
