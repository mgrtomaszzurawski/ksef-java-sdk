/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoiceDocument;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Internal construction bridge for typed {@link InvoiceDocument}
 * subtypes returned by the archive and session-cleared flows.
 *
 * <p>R1-5: the typed document factories
 * ({@code Fa2/Fa3/Pef/PefKorInvoiceDocument.from(byte[])}) and the
 * anonymous-wrapper helper are package-private inside
 * {@code sdk.domain.invoicing}. The SDK orchestrates document
 * construction from server-side XML; consumers read what the archive
 * returns. Keeping the factories package-private prevents leaking a
 * construction path on the published binary/Javadoc surface (mirrors
 * the {@code SessionHandleConstructor} pattern for session handles).
 *
 * <p>Cross-package access by SDK internals goes through this bridge.
 * The reflective lookups are cached as {@link Method} instances at
 * class-load time; subsequent {@code invoke} calls have negligible
 * overhead vs. direct invocation. Within the same named module,
 * {@link Method#setAccessible(boolean)} works without any
 * {@code opens} directive in {@code module-info.java}.
 *
 * @apiNote Internal — never call from consumer code.
 *
 * @since 1.0.0
 */
public final class InvoiceDocumentConstructor {

    private static final String ERR_REFLECTIVE_CONSTRUCTION_FAILED =
            "SDK internal error: reflective construction of invoice document failed";

    private static final String FROM_METHOD_NAME = "from";

    private static final String INVOICE_DOCUMENTS_FQN =
            "io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocuments";

    private static final String ANONYMOUS_METHOD_NAME = "anonymousFromXml";

    private static final Method FA2_FROM;
    private static final Method FA3_FROM;
    private static final Method PEF_FROM;
    private static final Method PEF_KOR_FROM;
    private static final Method ANONYMOUS_FROM;

    static {
        try {
            FA2_FROM = makeAccessible(Fa2InvoiceDocument.class.getDeclaredMethod(
                    FROM_METHOD_NAME, byte[].class));
            FA3_FROM = makeAccessible(Fa3InvoiceDocument.class.getDeclaredMethod(
                    FROM_METHOD_NAME, byte[].class));
            PEF_FROM = makeAccessible(PefInvoiceDocument.class.getDeclaredMethod(
                    FROM_METHOD_NAME, byte[].class));
            PEF_KOR_FROM = makeAccessible(PefKorInvoiceDocument.class.getDeclaredMethod(
                    FROM_METHOD_NAME, byte[].class));
            Class<?> invoiceDocumentsClass = Class.forName(INVOICE_DOCUMENTS_FQN);
            ANONYMOUS_FROM = makeAccessible(invoiceDocumentsClass.getDeclaredMethod(
                    ANONYMOUS_METHOD_NAME, FormCode.class, byte[].class));
        } catch (NoSuchMethodException | ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private InvoiceDocumentConstructor() { }

    @SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "java:S3011"})
    private static Method makeAccessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static Fa2InvoiceDocument newFa2Document(byte[] xml) {
        return invoke(FA2_FROM, Fa2InvoiceDocument.class, xml);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static Fa3InvoiceDocument newFa3Document(byte[] xml) {
        return invoke(FA3_FROM, Fa3InvoiceDocument.class, xml);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static PefInvoiceDocument newPefDocument(byte[] xml) {
        return invoke(PEF_FROM, PefInvoiceDocument.class, xml);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static PefKorInvoiceDocument newPefKorDocument(byte[] xml) {
        return invoke(PEF_KOR_FROM, PefKorInvoiceDocument.class, xml);
    }

    /**
     * @apiNote Internal — anonymous-wrapper variant for unknown / custom
     *     FormCode where the SDK has no schema to unmarshal against.
     *     Returns a minimal {@link InvoiceDocument} exposing only
     *     {@link InvoiceDocument#formCode()} and {@link InvoiceDocument#xml()}.
     */
    public static InvoiceDocument newAnonymousDocument(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, "formCode must not be null");
        Objects.requireNonNull(xml, "xml must not be null");
        try {
            return (InvoiceDocument) ANONYMOUS_FROM.invoke(null, formCode, xml);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ERR_REFLECTIVE_CONSTRUCTION_FAILED, ex);
        } catch (InvocationTargetException invocationFailure) {
            throw rethrow(invocationFailure);
        }
    }

    /**
     * Dispatch helper — given a known {@link FormCode}, returns the typed
     * subtype; for custom forms returns the anonymous wrapper. Centralises
     * the switch the archive and session flows used to inline.
     *
     * @apiNote Internal — see class-level Javadoc.
     */
    public static InvoiceDocument newDocument(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, "formCode must not be null");
        Objects.requireNonNull(xml, "xml must not be null");
        if (formCode.equals(FormCode.FA3)) {
            return newFa3Document(xml);
        }
        if (formCode.equals(FormCode.FA2)) {
            return newFa2Document(xml);
        }
        if (formCode.equals(FormCode.PEF3)) {
            return newPefDocument(xml);
        }
        if (formCode.equals(FormCode.PEF_KOR3)) {
            return newPefKorDocument(xml);
        }
        return newAnonymousDocument(formCode, xml);
    }

    private static <T> T invoke(Method method, Class<T> resultType, byte[] xml) {
        Objects.requireNonNull(xml, "xml must not be null");
        try {
            return resultType.cast(method.invoke(null, (Object) xml));
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ERR_REFLECTIVE_CONSTRUCTION_FAILED, ex);
        } catch (InvocationTargetException invocationFailure) {
            throw rethrow(invocationFailure);
        }
    }

    private static RuntimeException rethrow(InvocationTargetException invocationFailure) {
        Throwable cause = invocationFailure.getCause();
        if (cause instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        if (cause instanceof Error errorFailure) {
            throw errorFailure;
        }
        return new IllegalStateException(ERR_REFLECTIVE_CONSTRUCTION_FAILED, cause);
    }
}
