/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import java.util.Objects;

/**
 * Open contract for a KSeF invoice ready to be submitted.
 *
 * <p>An {@code Invoice} carries two pieces of information: the
 * {@link FormCode} declaring the schema the XML conforms to, and the
 * raw XML byte content. The interface is open so consumers can supply
 * their own implementation when the SDK does not (yet) ship a typed
 * builder for a given schema — KSeF accepts FA(2), FA(3), PEF(3) and
 * PEF_KOR(3) today, but {@link FormCode#custom(String, String, String)}
 * leaves room for future schemas the SDK has not modelled yet.
 *
 * <p>Use {@link #fromXml(FormCode, byte[])} as the minimal escape
 * hatch — it wraps a pre-rendered XML byte array. The per-schema typed
 * builders ({@link Fa2Invoice#builder()}, {@link Fa3Invoice#builder()},
 * {@link PefInvoice#builder()}, {@link PefKorInvoice#builder()}) also
 * implement this interface.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>return a non-null {@link FormCode} from {@link #formCode()};</li>
 *   <li>return a defensive copy of the underlying XML from
 *       {@link #xml()} so consumers cannot mutate the SDK's view of
 *       the invoice content after construction.</li>
 * </ul>
 *
 * <h2>Custom invoice types</h2>
 *
 * <p>The SDK ships typed builders for the four KSeF-recognised schemas
 * (FA(2), FA(3), PEF(3), PEF_KOR(3)). When KSeF adds a new schema before
 * the SDK ships matching typed support, write a thin
 * {@link Invoice} implementation against
 * {@link FormCode#custom(String, String, String, byte[]) FormCode.custom(...)}
 * and submit it through the normal session flow:
 *
 * <pre>{@code
 * byte[] myXsd = MyApp.class.getResourceAsStream("/xsd/my-schema.xsd").readAllBytes();
 * FormCode myForm = FormCode.custom("MY (1)", "1-0E", "MY", myXsd);
 *
 * final class MyCustomInvoice implements Invoice {
 *     private final byte[] xmlBytes;
 *     MyCustomInvoice(byte[] xmlBytes) { this.xmlBytes = xmlBytes.clone(); }
 *     @Override public FormCode formCode() { return myForm; }
 *     @Override public byte[] xml() { return xmlBytes.clone(); }
 * }
 *
 * try (var session = client.invoices().sessions().open(myForm)) {
 *     session.sendInvoice(new MyCustomInvoice(myXmlBytes));
 * }
 * }</pre>
 *
 * <p>Two SDK guarantees apply to custom forms:
 * <ul>
 *   <li>{@code FormCode.custom(..., xsdBytes)} drives client-side XSD
 *       validation (Phase 2 preflight) before the encrypted upload
 *       starts — the SDK does not transport invalid XML to KSeF.</li>
 *   <li>The four-arity {@code FormCode.custom(systemCode, schemaVersion, type, xsdBytes)}
 *       is the only form that enables XSD preflight; the three-arity
 *       overload skips Phase 2 and relies on server-side validation
 *       alone.</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface Invoice {

    /** Internal — error message for null formCode arguments. */
    String ERR_FORM_CODE_NULL = "formCode must not be null";
    /** Internal — error message for null xml arguments. */
    String ERR_XML_NULL = "xml must not be null";

    /**
     * The schema/form code declared for this invoice. Determines how
     * KSeF parses the XML payload.
     *
     * @return non-null form code
     */
    FormCode formCode();

    /**
     * The raw invoice XML bytes (unencrypted, defensive copy).
     *
     * <p>Implementations MUST return a fresh array on every call so
     * downstream mutation cannot affect the canonical content. The SDK
     * will encrypt these bytes with the session AES key before
     * submission — never include sensitive plaintext that should not
     * leave the session boundary.
     *
     * @return defensive copy of XML bytes
     */
    byte[] xml();

    /**
     * Minimal escape-hatch factory: wrap a {@code (formCode, xml)}
     * pair behind the {@link Invoice} contract.
     *
     * <p>Defensive-copies the XML on input AND on every {@link #xml()}
     * accessor call, so the returned object is fully insulated from
     * the caller's array. {@link #formCode()} returns the supplied
     * {@code formCode} as-is — {@code FormCode} itself is immutable.
     *
     * @param formCode non-null form code
     * @param invoiceXml non-null raw invoice XML bytes
     * @return immutable {@link Invoice} wrapper
     * @throws NullPointerException if either argument is null
     */
    static Invoice fromXml(FormCode formCode, byte[] invoiceXml) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(invoiceXml, ERR_XML_NULL);
        byte[] copy = invoiceXml.clone();
        return new Invoice() {
            @Override
            public FormCode formCode() {
                return formCode;
            }

            @Override
            public byte[] xml() {
                return copy.clone();
            }
        };
    }
}
