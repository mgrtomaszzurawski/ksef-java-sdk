/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.UnrecognizedInvoiceDocument;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-side registry of custom {@link InvoiceDocument} types the SDK
 * should construct when it encounters a non-built-in {@link FormCode}
 * (anything outside FA2/FA3/PEF/PEF_KOR).
 *
 * <p>The built-in schemas are pre-resolved by the SDK; this registry
 * extends the read-side flow with consumer-defined typed wrappers.
 * Returned documents flow through every read-side accessor that
 * routes through {@code InvoiceDocumentConstructor.newDocument} —
 * currently {@code client.invoices().archive().getByKsefNumber(...)},
 * {@code client.invoices().sync().asStream(...)}, and
 * {@code session.complete().cleared(submitted)}.
 *
 * <p><b>Convention each registered class must follow:</b>
 * <ul>
 *   <li>implements {@link InvoiceDocument}</li>
 *   <li>declares {@code public static final FormCode FORM_CODE = ...}</li>
 *   <li>declares {@code public static <Self> from(byte[] xml)} returning
 *       an instance of the same class</li>
 * </ul>
 *
 * Convention is validated at {@link Builder#register} time
 * ({@link IllegalArgumentException} on any violation), not at first
 * use — registration fails loud, not silently.
 *
 * <pre>{@code
 * public final class MyCustomInvoice implements InvoiceDocument {
 *     public static final FormCode FORM_CODE =
 *         FormCode.custom("MY-1", "1-0", "MyInvoice");
 *
 *     public static MyCustomInvoice from(byte[] xml) { ... }
 *
 *     @Override public FormCode formCode() { return FORM_CODE; }
 *     @Override public byte[] xml() { ... }
 *     // typed accessors here
 * }
 *
 * KsefInvoiceTypes types = KsefInvoiceTypes.builder()
 *     .register(MyCustomInvoice.class)
 *     .build();
 *
 * KsefClient client = KsefClient.builder()
 *     .environment(KsefEnvironment.DEMO)
 *     .credentials(credentials)
 *     .invoiceTypes(types)
 *     .build();
 * }</pre>
 *
 * <p>Documents whose {@link FormCode} matches neither a built-in nor a
 * registered custom type land on
 * {@link UnrecognizedInvoiceDocument} — pattern-match consumers handle
 * the unknown-form arm there.
 *
 * @since 1.0.0
 */
public final class KsefInvoiceTypes {

    private static final String ERR_NULL_TYPE = "type must not be null";
    private static final String ERR_NOT_INVOICE_DOCUMENT =
            "%s does not implement InvoiceDocument";
    private static final String ERR_MISSING_FORM_CODE_FIELD =
            "%s must declare 'public static final FormCode FORM_CODE'";
    private static final String ERR_FORM_CODE_WRONG_MODIFIERS =
            "%s.FORM_CODE must be public static final (got modifiers: %s)";
    private static final String ERR_FORM_CODE_WRONG_TYPE =
            "%s.FORM_CODE must be of type FormCode (got: %s)";
    private static final String ERR_FORM_CODE_VALUE_NULL =
            "%s.FORM_CODE is null at registration";
    private static final String ERR_MISSING_FROM_METHOD =
            "%s must declare 'public static %s from(byte[] xml)'";
    private static final String ERR_FROM_WRONG_MODIFIERS =
            "%s.from(byte[]) must be public static (got modifiers: %s)";
    private static final String ERR_FROM_WRONG_RETURN_TYPE =
            "%s.from(byte[]) must return %s (got: %s)";
    private static final String ERR_DUPLICATE_FORM_CODE =
            "FormCode %s is already registered for %s (cannot register %s)";
    private static final String ERR_REFLECTIVE_PARSE_FAILED =
            "Reflective parse failed for %s.from(byte[])";

    private static final String FORM_CODE_FIELD = "FORM_CODE";
    private static final String FROM_METHOD = "from";

    private final Map<FormCode, InvoiceTypeBinding> bindings;

    private KsefInvoiceTypes(Map<FormCode, InvoiceTypeBinding> bindings) {
        this.bindings = Map.copyOf(bindings);
    }

    /**
     * Start a builder pre-loaded with the SDK's built-in schemas
     * (FA2/FA3/PEF/PEF_KOR). Built-ins are resolved by the SDK
     * internally; they appear as legal but invisible entries — adding
     * a custom type for the same FormCode throws
     * {@link IllegalArgumentException} at registration. Use this when
     * extending the SDK's read-side with custom types.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Start a builder with no built-ins. Same effective registry as
     * {@link #builder()} from the consumer's perspective (built-ins
     * are always available on the read-side flow), but the builder
     * accepts custom types for FA2/FA3/PEF/PEF_KOR FormCodes which
     * normally collide with built-ins. Reserved for testing / mock
     * scenarios.
     */
    public static Builder builderFromScratch() {
        return new Builder();
    }

    /**
     * Zero-custom-type registry — equivalent to
     * {@code builder().build()}. The SDK's default when
     * {@code KsefClient.Builder.invoiceTypes(...)} is not invoked.
     */
    public static KsefInvoiceTypes builtinsOnly() {
        return new KsefInvoiceTypes(Map.of());
    }

    /**
     * Lookup the registered binding for the given form code, if any.
     * Returns empty when the form code is not registered as a custom
     * type — callers should fall back to built-in resolution before
     * concluding the form is unrecognised.
     */
    public Optional<InvoiceTypeBinding> binding(FormCode formCode) {
        return Optional.ofNullable(bindings.get(formCode));
    }

    /**
     * Fluent builder for {@link KsefInvoiceTypes}. Each
     * {@link #register} call validates the convention and caches a
     * {@link MethodHandle} bound to the class's
     * {@code public static from(byte[])} factory.
     */
    public static final class Builder {

        private final Map<FormCode, InvoiceTypeBinding> bindings = new HashMap<>();

        private Builder() { }

        /**
         * Register a custom {@link InvoiceDocument} type.
         *
         * @param type implementation class declaring
         *     {@code public static final FormCode FORM_CODE} and
         *     {@code public static <Self> from(byte[] xml)}
         * @return this builder
         * @throws IllegalArgumentException when the convention is
         *     violated or the form code is already bound
         */
        public Builder register(Class<? extends InvoiceDocument> type) {
            Objects.requireNonNull(type, ERR_NULL_TYPE);
            InvoiceTypeBinding binding = bindingFor(type);
            InvoiceTypeBinding previous = bindings.put(binding.formCode(), binding);
            if (previous != null) {
                bindings.put(binding.formCode(), previous);
                throw new IllegalArgumentException(String.format(ERR_DUPLICATE_FORM_CODE,
                        binding.formCode(), previous.type().getName(), type.getName()));
            }
            return this;
        }

        public KsefInvoiceTypes build() {
            return new KsefInvoiceTypes(bindings);
        }
    }

    private static InvoiceTypeBinding bindingFor(Class<? extends InvoiceDocument> type) {
        if (!InvoiceDocument.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format(ERR_NOT_INVOICE_DOCUMENT, type.getName()));
        }
        FormCode formCode = readFormCode(type);
        MethodHandle factory = resolveFromMethod(type);
        return new InvoiceTypeBinding(type, formCode, factory);
    }

    private static FormCode readFormCode(Class<? extends InvoiceDocument> type) {
        Field field;
        try {
            field = type.getDeclaredField(FORM_CODE_FIELD);
        } catch (NoSuchFieldException missing) {
            throw new IllegalArgumentException(String.format(ERR_MISSING_FORM_CODE_FIELD, type.getName()), missing);
        }
        int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
            throw new IllegalArgumentException(String.format(ERR_FORM_CODE_WRONG_MODIFIERS,
                    type.getName(), Modifier.toString(modifiers)));
        }
        if (!FormCode.class.equals(field.getType())) {
            throw new IllegalArgumentException(String.format(ERR_FORM_CODE_WRONG_TYPE,
                    type.getName(), field.getType().getName()));
        }
        try {
            FormCode value = (FormCode) field.get(null);
            if (value == null) {
                throw new IllegalArgumentException(String.format(ERR_FORM_CODE_VALUE_NULL, type.getName()));
            }
            return value;
        } catch (IllegalAccessException unexpected) {
            throw new IllegalArgumentException(String.format(ERR_FORM_CODE_WRONG_MODIFIERS,
                    type.getName(), Modifier.toString(modifiers)), unexpected);
        }
    }

    private static MethodHandle resolveFromMethod(Class<? extends InvoiceDocument> type) {
        Method method;
        try {
            method = type.getDeclaredMethod(FROM_METHOD, byte[].class);
        } catch (NoSuchMethodException missing) {
            throw new IllegalArgumentException(String.format(ERR_MISSING_FROM_METHOD,
                    type.getName(), type.getSimpleName()), missing);
        }
        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException(String.format(ERR_FROM_WRONG_MODIFIERS,
                    type.getName(), Modifier.toString(modifiers)));
        }
        if (!type.isAssignableFrom(method.getReturnType())) {
            throw new IllegalArgumentException(String.format(ERR_FROM_WRONG_RETURN_TYPE,
                    type.getName(), type.getSimpleName(), method.getReturnType().getName()));
        }
        try {
            return MethodHandles.lookup().unreflect(method)
                    .asType(MethodType.methodType(InvoiceDocument.class, byte[].class));
        } catch (IllegalAccessException unexpected) {
            throw new IllegalArgumentException(String.format(ERR_FROM_WRONG_MODIFIERS,
                    type.getName(), Modifier.toString(modifiers)), unexpected);
        }
    }

    /**
     * Resolved binding for a registered custom invoice type: caches the
     * {@link MethodHandle} bound to the type's
     * {@code public static from(byte[])} factory and exposes a
     * {@link #parse} dispatch helper.
     */
    public static final class InvoiceTypeBinding {

        private final Class<? extends InvoiceDocument> type;
        private final FormCode formCode;
        private final MethodHandle factory;

        InvoiceTypeBinding(Class<? extends InvoiceDocument> type, FormCode formCode, MethodHandle factory) {
            this.type = type;
            this.formCode = formCode;
            this.factory = factory;
        }

        public Class<? extends InvoiceDocument> type() {
            return type;
        }

        public FormCode formCode() {
            return formCode;
        }

        /**
         * Invoke the registered factory.
         *
         * @throws IllegalStateException if the factory itself fails
         *     ({@link InvocationTargetException} cause). Runtime
         *     exceptions thrown by user code propagate unchanged.
         */
        public InvoiceDocument parse(byte[] xml) {
            Objects.requireNonNull(xml, "xml must not be null");
            try {
                return (InvoiceDocument) factory.invokeExact(xml);
            } catch (RuntimeException userFailure) {
                throw userFailure;
            } catch (Throwable reflectiveFailure) {
                throw new IllegalStateException(
                        String.format(ERR_REFLECTIVE_PARSE_FAILED, type.getName()),
                        reflectiveFailure);
            }
        }
    }
}
