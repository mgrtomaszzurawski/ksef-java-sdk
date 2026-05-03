/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

/**
 * Invoice seller information from metadata.
 *
 * @param nip seller NIP (tax ID)
 * @param name seller name (may be null)
 */
public record InvoiceSeller(String nip, String name) {

}
