/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.invoicing.builder;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.SendInvoiceBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codex 2026-05-05 #11 — fail-fast guard against oversized invoice
 * payloads. KSeF spec limits a single invoice with attachments to
 * 3 MiB; before this check the SDK would encrypt and POST the payload
 * only to receive a server-side rejection.
 */
class SendInvoiceBuilderSizeGuardTest {

    private static final int MAX_INVOICE_BYTES = 3 * 1024 * 1024;
    private static final byte[] AES_KEY_FAKE = new byte[32];
    private static final byte[] IV_FAKE = new byte[16];

    @Test
    void create_belowMaxSize_succeeds() {
        byte[] xml = new byte[MAX_INVOICE_BYTES];
        SendInvoiceBuilder.create(xml, AES_KEY_FAKE, IV_FAKE);
    }

    @Test
    void create_aboveMaxSize_failsFast() {
        byte[] xml = new byte[MAX_INVOICE_BYTES + 1];
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SendInvoiceBuilder.create(xml, AES_KEY_FAKE, IV_FAKE));
        assertTrue(ex.getMessage().contains("exceeds spec limit"),
                "diagnostic should mention spec limit, was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(String.valueOf(MAX_INVOICE_BYTES + 1)),
                "diagnostic should report actual size, was: " + ex.getMessage());
    }
}
