/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * UBL CreditNoteLine model exposed by the read-side
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.PefKorInvoice} and
 * {@link io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.PefKorInvoiceDocument}
 * accessors. The data shape mirrors {@link PefInvoiceLine} but the field
 * names track the UBL CreditNote element {@code <cac:CreditNoteLine>},
 * which uses {@code <cbc:CreditedQuantity>} instead of
 * {@code <cbc:InvoicedQuantity>}.
 *
 * @param lineId line identifier (cbc:ID)
 * @param quantity credited quantity (cbc:CreditedQuantity)
 * @param unitCode UN/CEFACT unit code — e.g. {@code "C62"} (one),
 *     {@code "PCE"} (piece), {@code "HUR"} (hour)
 * @param lineExtensionAmount net amount for the line
 *     (cbc:LineExtensionAmount)
 * @param itemName goods / service name (cac:Item/cbc:Name)
 * @param vatPercent VAT percent
 *     (cac:Item/cac:ClassifiedTaxCategory/cbc:Percent)
 *
 * @since 1.0.0
 */
public record PefCreditNoteLine(
        String lineId,
        BigDecimal quantity,
        String unitCode,
        BigDecimal lineExtensionAmount,
        String itemName,
        BigDecimal vatPercent) {

    private static final String ERR_NULL_LINE_ID = "lineId must not be null";
    private static final String ERR_NULL_QUANTITY = "quantity must not be null";
    private static final String ERR_NULL_UNIT_CODE = "unitCode must not be null";
    private static final String ERR_NULL_LINE_EXTENSION_AMOUNT = "lineExtensionAmount must not be null";
    private static final String ERR_NULL_ITEM_NAME = "itemName must not be null";
    private static final String ERR_NULL_VAT_PERCENT = "vatPercent must not be null";

    public PefCreditNoteLine {
        Objects.requireNonNull(lineId, ERR_NULL_LINE_ID);
        Objects.requireNonNull(quantity, ERR_NULL_QUANTITY);
        Objects.requireNonNull(unitCode, ERR_NULL_UNIT_CODE);
        Objects.requireNonNull(lineExtensionAmount, ERR_NULL_LINE_EXTENSION_AMOUNT);
        Objects.requireNonNull(itemName, ERR_NULL_ITEM_NAME);
        Objects.requireNonNull(vatPercent, ERR_NULL_VAT_PERCENT);
    }
}
