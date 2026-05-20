/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes.InvoiceTypeBinding;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.InvoiceDocument;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the read-side registry: convention validation (8 documented
 * failure paths), duplicate rejection, happy-path lookup, and
 * {@link InvoiceTypeBinding#parse(byte[])} dispatch semantics
 * (RuntimeException pass-through vs reflective failure wrapping).
 */
class KsefInvoiceTypesTest {

    private static final FormCode CUSTOM_CODE_A =
            FormCode.custom("KIT-A", "1-0", "KitA");
    private static final FormCode CUSTOM_CODE_B =
            FormCode.custom("KIT-B", "1-0", "KitB");
    private static final byte[] SAMPLE_XML = "<KitA/>".getBytes(StandardCharsets.UTF_8);

    @Test
    void register_whenValidType_storesBindingWithCachedFactory() {
        KsefInvoiceTypes registry = KsefInvoiceTypes.builder()
                .register(ValidCustomInvoiceDocument.class)
                .build();

        Optional<InvoiceTypeBinding> binding = registry.binding(CUSTOM_CODE_A);
        assertTrue(binding.isPresent());
        assertSame(ValidCustomInvoiceDocument.class, binding.orElseThrow().type());
        assertEquals(CUSTOM_CODE_A, binding.orElseThrow().formCode());

        InvoiceDocument parsed = binding.orElseThrow().parse(SAMPLE_XML);
        assertEquals(CUSTOM_CODE_A, parsed.formCode());
        assertArrayEquals(SAMPLE_XML, parsed.xml());
        assertTrue(parsed instanceof ValidCustomInvoiceDocument);
    }

    @Test
    void binding_whenFormCodeUnknown_returnsEmpty() {
        KsefInvoiceTypes registry = KsefInvoiceTypes.builder().build();
        assertTrue(registry.binding(CUSTOM_CODE_A).isEmpty());
    }

    @Test
    void builtinsOnly_returnsEmptyRegistry() {
        KsefInvoiceTypes registry = KsefInvoiceTypes.builtinsOnly();
        assertNotNull(registry);
        assertTrue(registry.binding(CUSTOM_CODE_A).isEmpty());
    }

    @Test
    void register_whenNullType_throwsNullPointerException() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> builder.register(null));
        assertTrue(thrown.getMessage().contains("type"));
    }

    @Test
    void register_whenMissingFormCodeField_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(MissingFormCodeFieldInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains(MissingFormCodeFieldInvoiceDocument.class.getName()));
        assertTrue(thrown.getMessage().contains("FORM_CODE"));
    }

    @Test
    void register_whenFormCodeFieldNotPublicStaticFinal_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(NonFinalFormCodeInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("public static final"));
    }

    @Test
    void register_whenFormCodeWrongType_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(WrongFormCodeTypeInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("FormCode"));
    }

    @Test
    void register_whenFormCodeValueNull_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(NullFormCodeValueInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("null"));
    }

    @Test
    void register_whenMissingFromMethod_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(MissingFromMethodInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("from"));
    }

    @Test
    void register_whenFromMethodNotPublicStatic_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(InstanceFromMethodInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("public static"));
    }

    @Test
    void register_whenFromMethodWrongReturnType_throwsIllegalArgument() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder();
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(WrongReturnTypeFromMethodInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains("return"));
    }

    @Test
    void register_whenSameFormCodeRegisteredTwice_throwsAndPreservesFirst() {
        KsefInvoiceTypes.Builder builder = KsefInvoiceTypes.builder()
                .register(ValidCustomInvoiceDocument.class);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> builder.register(DuplicateFormCodeInvoiceDocument.class));
        assertTrue(thrown.getMessage().contains(CUSTOM_CODE_A.toString()),
                () -> "Error should reference the duplicate FormCode: " + thrown.getMessage());

        KsefInvoiceTypes registry = builder.build();
        Optional<InvoiceTypeBinding> binding = registry.binding(CUSTOM_CODE_A);
        assertTrue(binding.isPresent());
        assertSame(ValidCustomInvoiceDocument.class, binding.orElseThrow().type(),
                "First registration must win after a duplicate attempt.");
    }

    @Test
    void parse_whenUserFactoryThrowsRuntimeException_propagatesUnchanged() {
        KsefInvoiceTypes registry = KsefInvoiceTypes.builder()
                .register(ThrowingFactoryInvoiceDocument.class)
                .build();
        InvoiceTypeBinding binding = registry.binding(CUSTOM_CODE_B).orElseThrow();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> binding.parse(SAMPLE_XML));
        assertEquals("user-failure", thrown.getMessage());
    }

    // -------- Fixtures --------

    /** Valid: public InvoiceDocument with public static final FORM_CODE + public static from(byte[]). */
    public static final class ValidCustomInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_A;
        private final byte[] xml;
        private ValidCustomInvoiceDocument(byte[] xml) {
            this.xml = xml.clone();
        }
        public static ValidCustomInvoiceDocument from(byte[] xml) {
            return new ValidCustomInvoiceDocument(xml);
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return xml.clone();
        }
    }

    /** Second valid implementation declaring the SAME FormCode — used to verify duplicate-rejection. */
    public static final class DuplicateFormCodeInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_A;
        public static DuplicateFormCodeInvoiceDocument from(byte[] xml) {
            return new DuplicateFormCodeInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** Missing FORM_CODE field. */
    public static final class MissingFormCodeFieldInvoiceDocument implements InvoiceDocument {
        public static MissingFormCodeFieldInvoiceDocument from(byte[] xml) {
            return new MissingFormCodeFieldInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return CUSTOM_CODE_A;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** FORM_CODE declared but not final. */
    public static final class NonFinalFormCodeInvoiceDocument implements InvoiceDocument {
        @SuppressWarnings("PMD.MutableStaticState")
        public static FormCode FORM_CODE = CUSTOM_CODE_A;
        public static NonFinalFormCodeInvoiceDocument from(byte[] xml) {
            return new NonFinalFormCodeInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** FORM_CODE declared with wrong type (String instead of FormCode). */
    public static final class WrongFormCodeTypeInvoiceDocument implements InvoiceDocument {
        public static final String FORM_CODE = "KIT-A";
        public static WrongFormCodeTypeInvoiceDocument from(byte[] xml) {
            return new WrongFormCodeTypeInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return CUSTOM_CODE_A;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** FORM_CODE evaluated to null at registration time. */
    public static final class NullFormCodeValueInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = null;
        public static NullFormCodeValueInvoiceDocument from(byte[] xml) {
            return new NullFormCodeValueInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** Missing from(byte[]) factory entirely. */
    public static final class MissingFromMethodInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_A;
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** from(byte[]) exists but is an instance method, not static. */
    public static final class InstanceFromMethodInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_A;
        public InstanceFromMethodInvoiceDocument from(byte[] xml) {
            return new InstanceFromMethodInvoiceDocument();
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** from(byte[]) exists but returns a different type. */
    public static final class WrongReturnTypeFromMethodInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_A;
        public static String from(byte[] xml) {
            return "not-an-invoice-document";
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }

    /** Valid registration but the from() factory throws — verifies RuntimeException pass-through. */
    public static final class ThrowingFactoryInvoiceDocument implements InvoiceDocument {
        public static final FormCode FORM_CODE = CUSTOM_CODE_B;
        public static ThrowingFactoryInvoiceDocument from(byte[] xml) {
            throw new IllegalStateException("user-failure");
        }
        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }
        @Override
        public byte[] xml() {
            return new byte[0];
        }
    }
}
