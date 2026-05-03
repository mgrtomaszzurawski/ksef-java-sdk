/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Invoice form code identifying the schema version.
 *
 * @param systemCode system code (e.g. "FA")
 * @param schemaVersion schema version (e.g. "2")
 * @param value full form code value
 */
public record FormCodeInfo(String systemCode, String schemaVersion, String value) {

}
