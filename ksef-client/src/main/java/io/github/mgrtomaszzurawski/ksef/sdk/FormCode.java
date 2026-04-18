/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import java.util.Objects;

/**
 * Invoice form code identifying the schema used for invoice submission.
 *
 * <p>Each form code is a triplet of (systemCode, schemaVersion, value) that tells KSeF
 * which invoice format to expect in the session.
 *
 * <p>Common values:
 * <ul>
 *   <li>{@link #FA2} — FA(2) invoice, the most common format</li>
 *   <li>{@link #FA3} — FA(3) invoice</li>
 * </ul>
 *
 * <p>For invoice types not covered by the predefined constants, use
 * {@link #custom(String, String, String)}.
 */
public final class FormCode {

    private static final String ERR_NULL_SYSTEM_CODE = "systemCode must not be null";
    private static final String ERR_NULL_SCHEMA_VERSION = "schemaVersion must not be null";
    private static final String ERR_NULL_VALUE = "value must not be null";

    /** FA(2) — standard invoice, schema version 2. */
    public static final FormCode FA2 = new FormCode("FA", "2", "FA (2)");

    /** FA(3) — standard invoice, schema version 3. */
    public static final FormCode FA3 = new FormCode("FA", "3", "FA (3)");

    private final String systemCode;
    private final String schemaVersion;
    private final String value;

    private FormCode(String systemCode, String schemaVersion, String value) {
        this.systemCode = Objects.requireNonNull(systemCode, ERR_NULL_SYSTEM_CODE);
        this.schemaVersion = Objects.requireNonNull(schemaVersion, ERR_NULL_SCHEMA_VERSION);
        this.value = Objects.requireNonNull(value, ERR_NULL_VALUE);
    }

    /**
     * Create a custom form code for invoice types not covered by predefined constants.
     *
     * @param systemCode e.g. "FA", "PEF", "RR"
     * @param schemaVersion e.g. "2", "3"
     * @param value e.g. "FA (2)", "PEF (3)"
     * @return custom form code
     */
    public static FormCode custom(String systemCode, String schemaVersion, String value) {
        return new FormCode(systemCode, schemaVersion, value);
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
