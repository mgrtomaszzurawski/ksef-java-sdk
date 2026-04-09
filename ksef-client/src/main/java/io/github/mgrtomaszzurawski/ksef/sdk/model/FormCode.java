/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.model;

import io.github.mgrtomaszzurawski.ksef.client.model.FormCodeRaw;

/**
 * Invoice form code identifying the schema version.
 *
 * @param systemCode system code (e.g. "FA")
 * @param schemaVersion schema version (e.g. "2")
 * @param value full form code value
 */
public record FormCode(String systemCode, String schemaVersion, String value) {

    public static FormCode from(FormCodeRaw raw) {
        if (raw == null) {
            return null;
        }
        return new FormCode(raw.getSystemCode(), raw.getSchemaVersion(), raw.getValue());
    }
}
