/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import org.jspecify.annotations.Nullable;

/**
 * One registry reference for the issuer in an external register — the
 * typed view of a {@code Faktura/Stopka/Rejestry} entry. Every member is
 * optional in the XSD.
 *
 * @param fullName issuer full name as registered ({@code PelnaNazwa}); null when not supplied
 * @param krs National Court Register number ({@code KRS}); null when not supplied
 * @param regon REGON statistical number ({@code REGON}); null when not supplied
 * @param bdo waste-database (BDO) number ({@code BDO}); null when not supplied
 *
 * @since 0.1.0
 */
public record RegistryEntry(
        @Nullable String fullName,
        @Nullable String krs,
        @Nullable String regon,
        @Nullable String bdo) {
}
