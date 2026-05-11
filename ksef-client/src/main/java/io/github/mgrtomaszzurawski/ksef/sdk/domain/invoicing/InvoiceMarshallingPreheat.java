/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator;

/**
 * Static bridge that lets {@link io.github.mgrtomaszzurawski.ksef.sdk.KsefClient#warmup()}
 * trigger the package-private {@link JaxbInvoiceMarshaller#warmupAll()} from
 * outside this package while keeping the marshaller itself
 * package-private. Also kicks the public
 * {@link KsefXmlValidator#warmupAll()} so the JAXB and the XSD caches are
 * primed together — the consumer expects one call to pay both costs.
 *
 * <p>Internal — consumers call {@code KsefClient.warmup()}, not this class.
 *
 * @since 1.0.0
 */
public final class InvoiceMarshallingPreheat {

    private InvoiceMarshallingPreheat() {
    }

    /**
     * Trigger lazy JAXBContext construction for every shipped invoice root
     * + bundled XSD load for every supported form code. Idempotent.
     */
    public static void preheatAll() {
        JaxbInvoiceMarshaller.warmupAll();
        KsefXmlValidator.warmupAll();
    }
}
