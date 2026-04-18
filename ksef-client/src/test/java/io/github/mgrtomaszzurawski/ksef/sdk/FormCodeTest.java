/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormCodeTest {

    private static final String SYSTEM_CODE_FA = "FA";
    private static final String SCHEMA_VERSION_2 = "2";
    private static final String SCHEMA_VERSION_3 = "3";
    private static final String VALUE_FA2 = "FA (2)";
    private static final String VALUE_FA3 = "FA (3)";
    private static final String SYSTEM_CODE_PEF = "PEF";
    private static final String VALUE_PEF3 = "PEF (3)";

    @Test
    void fa2_hasExpectedValues() {
        // given
        FormCode formCode = FormCode.FA2;

        // then
        assertEquals(SYSTEM_CODE_FA, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_2, formCode.schemaVersion());
        assertEquals(VALUE_FA2, formCode.value());
    }

    @Test
    void fa3_hasExpectedValues() {
        // given
        FormCode formCode = FormCode.FA3;

        // then
        assertEquals(SYSTEM_CODE_FA, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_3, formCode.schemaVersion());
        assertEquals(VALUE_FA3, formCode.value());
    }

    @Test
    void custom_whenValidInputs_createsSuccessfully() {
        // when
        FormCode formCode = FormCode.custom(SYSTEM_CODE_PEF, SCHEMA_VERSION_3, VALUE_PEF3);

        // then
        assertEquals(SYSTEM_CODE_PEF, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_3, formCode.schemaVersion());
        assertEquals(VALUE_PEF3, formCode.value());
    }

    @Test
    void custom_whenNullSystemCode_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(null, SCHEMA_VERSION_2, VALUE_FA2));
    }

    @Test
    void custom_whenNullSchemaVersion_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(SYSTEM_CODE_FA, null, VALUE_FA2));
    }

    @Test
    void custom_whenNullValue_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(SYSTEM_CODE_FA, SCHEMA_VERSION_2, null));
    }

    @Test
    void equals_whenSameValues_returnsTrue() {
        // given
        FormCode custom = FormCode.custom(SYSTEM_CODE_FA, SCHEMA_VERSION_2, VALUE_FA2);

        // when / then
        assertEquals(FormCode.FA2, custom);
    }

    @Test
    void equals_whenDifferentValues_returnsFalse() {
        // given / when / then
        assertNotEquals(FormCode.FA2, FormCode.FA3);
    }

    @Test
    void hashCode_whenEqualObjects_returnsSameHash() {
        // given
        FormCode custom = FormCode.custom(SYSTEM_CODE_FA, SCHEMA_VERSION_2, VALUE_FA2);

        // when / then
        assertEquals(FormCode.FA2.hashCode(), custom.hashCode());
    }

    @Test
    void toString_returnsValue() {
        // when
        String result = FormCode.FA2.toString();

        // then
        assertEquals(VALUE_FA2, result);
    }
}
