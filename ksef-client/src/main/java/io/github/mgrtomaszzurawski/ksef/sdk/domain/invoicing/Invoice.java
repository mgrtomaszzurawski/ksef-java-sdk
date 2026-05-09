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
 * hatch — it wraps a pre-rendered XML byte array. PR12b will add
 * per-schema typed builders ({@code Fa3Invoice.builder()} etc.) that
 * also implement this interface.
 *
 * <p>Implementations must:
 * <ul>
 *   <li>return a non-null {@link FormCode} from {@link #formCode()};</li>
 *   <li>return a defensive copy of the underlying XML from
 *       {@link #xml()} so consumers cannot mutate the SDK's view of
 *       the invoice content after construction.</li>
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
     * @param xml non-null raw invoice XML bytes
     * @return immutable {@link Invoice} wrapper
     * @throws NullPointerException if either argument is null
     */
    static Invoice fromXml(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, ERR_FORM_CODE_NULL);
        Objects.requireNonNull(xml, ERR_XML_NULL);
        byte[] copy = xml.clone();
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
