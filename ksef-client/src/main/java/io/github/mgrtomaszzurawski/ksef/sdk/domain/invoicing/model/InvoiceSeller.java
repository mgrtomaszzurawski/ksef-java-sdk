/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Invoice seller information from metadata.
 *
 * @param nip seller NIP (tax ID)
 * @param name seller name (may be null)
 *
 * @since 1.0.0
 */
public record InvoiceSeller(String nip, String name) {

}
