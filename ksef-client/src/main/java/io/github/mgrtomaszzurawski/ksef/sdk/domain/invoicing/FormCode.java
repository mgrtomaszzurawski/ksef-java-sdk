/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Invoice form code identifying the schema used for invoice submission.
 *
 * <p>Each form code is a triplet of {@code (systemCode, schemaVersion, value)}
 * that tells KSeF which invoice format to expect in the session.
 *
 * <p>Predefined constants (per {@code ksef-docs/srodowiska.md} and the
 * {@code open-api.json} {@code FormCodeRequest} schema):
 * <ul>
 *   <li>{@link #FA2} — FA(2) standard invoice; accepted on TEST only.</li>
 *   <li>{@link #FA3} — FA(3) standard invoice; accepted on TEST/DEMO/PROD.</li>
 *   <li>{@link #PEF3} — PEF(3) public-procurement invoice.</li>
 *   <li>{@link #PEF_KOR3} — PEF_KOR(3) PEF correction invoice.</li>
 * </ul>
 *
 * <p>For invoice types not covered by the predefined constants use
 * {@link #custom(String, String, String)} — the SDK transports such
 * invoices but skips client-side XSD validation (KSeF server remains
 * authoritative). To shift validation left for a custom invoice type,
 * use {@link #custom(String, String, String, byte[])} and supply the
 * XSD bytes; {@code KsefXmlValidator} and the internal pre-flight gate
 * then run the same validation pipeline used for bundled forms.
 *
 * @since 1.0.0
 */
public final class FormCode {

    private static final String ERR_NULL_SYSTEM_CODE = "systemCode must not be null";
    private static final String ERR_NULL_SCHEMA_VERSION = "schemaVersion must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";
    private static final String ERR_NULL_XSD_BYTES = "xsdBytes must not be null";
    private static final String ERR_NULL_ENVIRONMENT = "environment must not be null";
    private static final String ERR_FA2_NOT_ALLOWED =
            "FA(2) is accepted only on the TEST environment; DEMO and PROD reject FA(2). "
                    + "Use FormCode.FA3 (or FormCode.PEF3 / FormCode.PEF_KOR3) instead.";

    /** FA(2) — standard invoice, schema 1-0E. TEST environment only. */
    public static final FormCode FA2 = new FormCode("FA (2)", "1-0E", "FA");

    /** FA(3) — standard invoice, schema 1-0E. */
    public static final FormCode FA3 = new FormCode("FA (3)", "1-0E", "FA");

    /**
     * PEF(3) — public-procurement invoice (UBL/Peppol), schema 2-1.
     * The session-open systemCode is {@code "PEF (3)"} per the
     * canonical KSeF wire contract (matches upstream
     * {@code CIRFMF/ksef-client-java SystemCode.PEF_3}). Accepted on
     * TEST/DEMO/PROD when authenticated as a Peppol provider with
     * {@code PefInvoiceWrite} permission. {@code srodowiska.md} lists
     * the human-readable label {@code "FA_PEF (3)"} for the
     * <em>document type</em>, but the wire-level systemCode in the
     * open-session request stays {@code "PEF (3)"}.
     */
    public static final FormCode PEF3 = new FormCode("PEF (3)", "2-1", "PEF");

    /**
     * PEF_KOR(3) — PEF correction invoice, schema 2-1. See {@link #PEF3}
     * for the systemCode-vs-document-type-label note.
     */
    public static final FormCode PEF_KOR3 = new FormCode("PEF_KOR (3)", "2-1", "PEF");

    private final String systemCode;
    private final String schemaVersion;
    private final String value;
    @Nullable private final byte[] customXsdBytes;

    private FormCode(String systemCode, String schemaVersion, String value) {
        this(systemCode, schemaVersion, value, null);
    }

    private FormCode(String systemCode, String schemaVersion, String value,
                     @Nullable byte[] customXsdBytes) {
        this.systemCode = Objects.requireNonNull(systemCode, ERR_NULL_SYSTEM_CODE);
        this.schemaVersion = Objects.requireNonNull(schemaVersion, ERR_NULL_SCHEMA_VERSION);
        this.value = Objects.requireNonNull(value, ERR_NULL_VALUE);
        this.customXsdBytes = customXsdBytes == null ? null : customXsdBytes.clone();
    }

    /**
     * Create a custom form code for invoice types not covered by predefined
     * constants. No XSD is attached — client-side validation is skipped for
     * the resulting form; KSeF server validation remains authoritative. Use
     * {@link #custom(String, String, String, byte[])} to attach an XSD and
     * enable client-side pre-flight validation.
     *
     * <p>Field positions match the canonical {@code FormCodeRequest} schema:
     *
     * @param systemCode full system identifier — e.g. {@code "FA (3)"},
     *     {@code "PEF (3)"}, {@code "PEF_KOR (3)"}
     * @param schemaVersion XSD-level version string — e.g. {@code "1-0E"}
     *     for FA(3), {@code "2-1"} for PEF(3)
     * @param value short type tag — e.g. {@code "FA"}, {@code "PEF"}
     * @return custom form code with no attached XSD
     */
    public static FormCode custom(String systemCode, String schemaVersion, String value) {
        return new FormCode(systemCode, schemaVersion, value);
    }

    /**
     * Create a custom form code with an attached XSD. {@code KsefXmlValidator}
     * and the internal pre-flight gate use the supplied bytes to validate
     * outgoing invoice XML before submission, just like for the bundled
     * predefined form codes. Defensive copy on input and on every internal
     * access so the caller's array cannot be mutated post-construction.
     *
     * <p>Typical usage at application startup:
     * <pre>{@code
     * private static final byte[] MY_XSD =
     *         MyApp.class.getResourceAsStream("/xsd/my-schema.xsd").readAllBytes();
     * private static final FormCode MY_FORM =
     *         FormCode.custom("MY (1)", "1-0E", "FA", MY_XSD);
     * }</pre>
     *
     * @param systemCode    full system identifier
     * @param schemaVersion XSD-level version string
     * @param value         short type tag
     * @param xsdBytes      XSD document bytes used for client-side
     *                      validation; defensively copied
     * @return custom form code with attached XSD
     * @throws NullPointerException if any argument is null
     */
    public static FormCode custom(String systemCode, String schemaVersion, String value,
                                  byte[] xsdBytes) {
        Objects.requireNonNull(xsdBytes, ERR_NULL_XSD_BYTES);
        return new FormCode(systemCode, schemaVersion, value, xsdBytes);
    }

    /**
     * @apiNote Internal — used by {@code KsefXmlValidator} and
     *     {@code InvoiceValidationGate} to drive client-side validation
     *     for custom form codes; not part of the consumer contract.
     *
     * <p>Returns a defensive copy of the attached XSD bytes, or
     * {@code null} when the form code was created without an attached
     * XSD ({@link #custom(String, String, String)} or one of the
     * predefined constants).
     *
     * @return XSD bytes (defensive copy) or {@code null}
     */
    @Nullable
    public byte[] customXsdBytes() {
        return customXsdBytes == null ? null : customXsdBytes.clone();
    }

    public String systemCode() {
        return systemCode;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String value() {
        return value;
    }

    /**
     * Throw {@link IllegalArgumentException} when this form code is not
     * accepted on the supplied environment. Per
     * {@code ksef-docs/srodowiska.md}, FA(2) is accepted only on
     * {@link KsefEnvironment#TEST}; DEMO and PROD accept FA(3), PEF(3)
     * and PEF_KOR(3) only. Custom form codes are not checked (the SDK
     * cannot infer their environment policy) and pass through.
     *
     * <p>Called by {@code client.invoices().sessions().open} and
     * {@code client.invoices().batch().submit*} as a client-side preflight so misconfigured
     * consumers fail fast with a clear message instead of seeing a
     * server-side schema rejection on the first invoice send.
     */
    public void assertAllowedOn(KsefEnvironment environment) {
        Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
        if (!this.equals(FA2)) {
            return;
        }
        if (environment.equals(KsefEnvironment.DEMO)
                || environment.equals(KsefEnvironment.PROD)) {
            throw new IllegalArgumentException(ERR_FA2_NOT_ALLOWED);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FormCode other)) {
            return false;
        }
        return Objects.equals(systemCode, other.systemCode)
                && Objects.equals(schemaVersion, other.schemaVersion)
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemCode, schemaVersion, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
