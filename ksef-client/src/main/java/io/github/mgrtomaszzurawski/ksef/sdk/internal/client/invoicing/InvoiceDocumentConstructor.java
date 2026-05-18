/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.UnrecognizedInvoiceDocument;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Internal construction bridge for typed {@link InvoiceDocument}
 * subtypes returned by the archive, sync, and session-cleared flows.
 *
 * <p>R1-5: the typed document factories
 * ({@code Fa2/Fa3/Pef/PefKorInvoiceDocument.from(byte[])}) are
 * package-private inside {@code sdk.domain.invoicing}. The SDK
 * orchestrates document construction from server-side XML; consumers
 * read what the archive returns. Keeping the factories package-private
 * prevents leaking a construction path on the published binary/Javadoc
 * surface (mirrors the {@code SessionHandleConstructor} pattern for
 * session handles).
 *
 * <p>R2-6 ext: dispatch now consults a {@link KsefInvoiceTypes}
 * registry between the hardcoded built-in fast-path and the
 * {@link UnrecognizedInvoiceDocument} fallback. Built-ins always win
 * (consumers cannot replace FA2/FA3/PEF/PEF_KOR via the registry —
 * registration of those FormCodes is rejected by
 * {@link KsefInvoiceTypes.Builder#register}). Cross-package access by
 * SDK internals goes through this bridge.
 *
 * <p>The reflective lookups for built-ins are cached as {@link Method}
 * instances at class-load time; subsequent {@code invoke} calls have
 * negligible overhead vs. direct invocation. Within the same named
 * module, {@link Method#setAccessible(boolean)} works without any
 * {@code opens} directive in {@code module-info.java}.
 *
 * @apiNote Internal — never call from consumer code.
 *
 * @since 1.0.0
 */
public final class InvoiceDocumentConstructor {

    private static final String ERR_REFLECTIVE_CONSTRUCTION_FAILED =
            "SDK internal error: reflective construction of invoice document failed";
    private static final String ERR_NULL_FORM_CODE = "formCode must not be null";
    private static final String ERR_NULL_XML = "xml must not be null";
    private static final String ERR_NULL_REGISTRY = "registry must not be null";

    private static final String FROM_METHOD_NAME = "from";

    private static final Method FA2_FROM;
    private static final Method FA3_FROM;
    private static final Method PEF_FROM;
    private static final Method PEF_KOR_FROM;

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
        } catch (NoSuchMethodException ex) {
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
     * @apiNote Internal — typed-fallback variant for unknown / custom
     *     FormCode where the SDK has no schema to unmarshal against.
     *     Returns an {@link UnrecognizedInvoiceDocument} exposing only
     *     {@link InvoiceDocument#formCode()} and
     *     {@link InvoiceDocument#xml()}.
     */
    public static InvoiceDocument newAnonymousDocument(FormCode formCode, byte[] xml) {
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        Objects.requireNonNull(xml, ERR_NULL_XML);
        return new UnrecognizedInvoiceDocument(formCode, xml);
    }

    /**
     * Dispatch helper using the SDK's default (built-ins-only) registry.
     * Equivalent to {@code newDocument(KsefInvoiceTypes.builtinsOnly(),
     * formCode, xml)}.
     *
     * @apiNote Internal — see class-level Javadoc.
     */
    public static InvoiceDocument newDocument(FormCode formCode, byte[] xml) {
        return newDocument(KsefInvoiceTypes.builtinsOnly(), formCode, xml);
    }

    /**
     * Dispatch helper — given a {@link FormCode}, returns:
     * <ol>
     *   <li>the typed built-in subtype when the form is one of
     *       FA2/FA3/PEF/PEF_KOR;</li>
     *   <li>the typed wrapper produced by the user-registered factory
     *       when {@code registry} carries a binding for the form;</li>
     *   <li>an {@link UnrecognizedInvoiceDocument} otherwise.</li>
     * </ol>
     *
     * @apiNote Internal — see class-level Javadoc.
     */
    public static InvoiceDocument newDocument(KsefInvoiceTypes registry, FormCode formCode, byte[] xml) {
        Objects.requireNonNull(registry, ERR_NULL_REGISTRY);
        Objects.requireNonNull(formCode, ERR_NULL_FORM_CODE);
        Objects.requireNonNull(xml, ERR_NULL_XML);
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
        return registry.binding(formCode)
                .map(binding -> binding.parse(xml))
                .orElseGet(() -> new UnrecognizedInvoiceDocument(formCode, xml));
    }

    private static <T> T invoke(Method method, Class<T> resultType, byte[] xml) {
        Objects.requireNonNull(xml, ERR_NULL_XML);
        try {
            return resultType.cast(method.invoke(null, xml));
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
