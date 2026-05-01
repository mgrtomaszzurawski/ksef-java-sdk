/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormCodeTest {

    private static final String SYSTEM_CODE_FA2 = "FA (2)";
    private static final String SYSTEM_CODE_FA3 = "FA (3)";
    private static final String SCHEMA_VERSION_FA = "1-0E";
    private static final String VALUE_FA = "FA";
    private static final String SYSTEM_CODE_PEF3 = "PEF (3)";
    private static final String SCHEMA_VERSION_PEF = "2-1";
    private static final String VALUE_PEF = "PEF";

    @Test
    void fa2_hasExpectedValues() {
        // given
        FormCode formCode = FormCode.FA2;

        // then
        assertEquals(SYSTEM_CODE_FA2, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_FA, formCode.schemaVersion());
        assertEquals(VALUE_FA, formCode.value());
    }

    @Test
    void fa3_hasExpectedValues() {
        // given
        FormCode formCode = FormCode.FA3;

        // then
        assertEquals(SYSTEM_CODE_FA3, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_FA, formCode.schemaVersion());
        assertEquals(VALUE_FA, formCode.value());
    }

    @Test
    void custom_whenValidInputs_createsSuccessfully() {
        // when
        FormCode formCode = FormCode.custom(SYSTEM_CODE_PEF3, SCHEMA_VERSION_PEF, VALUE_PEF);

        // then
        assertEquals(SYSTEM_CODE_PEF3, formCode.systemCode());
        assertEquals(SCHEMA_VERSION_PEF, formCode.schemaVersion());
        assertEquals(VALUE_PEF, formCode.value());
    }

    @Test
    void custom_whenNullSystemCode_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(null, SCHEMA_VERSION_FA, VALUE_FA));
    }

    @Test
    void custom_whenNullSchemaVersion_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(SYSTEM_CODE_FA2, null, VALUE_FA));
    }

    @Test
    void custom_whenNullValue_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class,
                () -> FormCode.custom(SYSTEM_CODE_FA2, SCHEMA_VERSION_FA, null));
    }

    @Test
    void equals_whenSameValues_returnsTrue() {
        // given
        FormCode custom = FormCode.custom(SYSTEM_CODE_FA2, SCHEMA_VERSION_FA, VALUE_FA);

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
        FormCode custom = FormCode.custom(SYSTEM_CODE_FA2, SCHEMA_VERSION_FA, VALUE_FA);

        // when / then
        assertEquals(FormCode.FA2.hashCode(), custom.hashCode());
    }

    @Test
    void toString_returnsValue() {
        // when
        String result = FormCode.FA2.toString();

        // then
        assertEquals(VALUE_FA, result);
    }
}
