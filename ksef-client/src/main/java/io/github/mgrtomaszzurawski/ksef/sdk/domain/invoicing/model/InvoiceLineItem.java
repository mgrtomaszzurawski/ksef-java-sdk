/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/**
 * Single line item shared between the FA(2) and FA(3) typed builders.
 *
 * <p>Models the full scalar surface of a {@code FaWiersz} element. Every
 * scalar the FA(2)/FA(3) schema declares directly on {@code FaWiersz} is
 * surfaced here — the read path never forces a consumer to the
 * {@code unsafeJaxbView()} escape hatch for a line-item scalar.
 *
 * <p>The fields are flat because {@code FaWiersz} declares them flat in the
 * XSD; per the coverage doctrine the typed overlay mirrors the schema
 * structure rather than inventing groupings. Construct via {@link #builder()}
 * — the canonical 23-argument constructor exists for internal mapping.
 *
 * <p><strong>Nullability follows the FA(2)/FA(3) XSD.</strong> Every scalar on
 * {@code FaWiersz} is {@code minOccurs="0"}, so a valid line may carry, for
 * example, only a gross amount ({@code P_11A}) with no net ({@code P_11}).
 * Every {@code FaWiersz} maps to exactly one {@code InvoiceLineItem}; the read
 * path never drops a line, so the accessors below return {@code null} where
 * the source element was absent.
 *
 * @param rowNumber row ordinal — 1-based, {@code NrWierszaFa}
 * @param description product / service description ({@code P_7}) — may be null
 * @param gtin GTIN / barcode ({@code GTIN}) — may be null
 * @param pkwiu PKWiU classification ({@code PKWiU}) — may be null
 * @param unitOfMeasure unit symbol ({@code P_8A}) — may be null
 * @param quantity quantity ({@code P_8B}) — may be null
 * @param netUnitPrice unit price net ({@code P_9A}) — may be null
 * @param netAmount total net amount ({@code P_11}) — may be null on a
 *     gross-only line
 * @param vatRate VAT rate token ({@code P_12}) — e.g. {@code "23"}, {@code "8"},
 *     {@code "5"}, {@code "0"}, {@code "zw"}, {@code "np"} — may be null
 * @param grossAmount total gross amount ({@code P_11A}) — may be null
 * @param vatAmount VAT amount ({@code P_11Vat}) — may be null
 * @param deliveryDate date of delivery / service completion for the line
 *     ({@code P_6A}) — may be null
 * @param uuid line-item UUID ({@code UUID}) — may be null
 * @param index internal product index ({@code Indeks}) — may be null
 * @param cnCode CN customs classification ({@code CN}) — may be null
 * @param pkobCode PKOB building classification ({@code PKOB}) — may be null
 * @param grossUnitPrice unit price gross ({@code P_9B}) — may be null
 * @param discountAmount discounts / rebates amount ({@code P_10}) — may be null
 * @param exciseAmount excise amount ({@code KwotaAkcyzy}) — may be null
 * @param exchangeRate currency exchange rate for the line ({@code KursWaluty})
 *     — may be null
 * @param valueAddedTaxRate value-added-tax rate for the OSS procedure
 *     ({@code P_12_XII}) — may be null
 * @param annex15 {@code true} when the line covers goods / services listed in
 *     Annex 15 — mandatory split payment ({@code P_12_Zal_15}, marker
 *     {@code 1}) — may be null
 * @param correctionStateBefore {@code true} when the line represents the
 *     pre-correction state ({@code StanPrzed}, marker {@code 1}) — may be null
 * @param gtuCode GTU goods/services group marking ({@code GTU}), e.g.
 *     {@code "GTU_01"} — may be null
 * @param procedureMarking special procedure marking ({@code Procedura}), e.g.
 *     {@code "WSTO_EE"}, {@code "IED"} — may be null
 *
 * @since 0.1.0
 */
public record InvoiceLineItem(
        int rowNumber,
        @Nullable String description,
        @Nullable String gtin,
        @Nullable String pkwiu,
        @Nullable String unitOfMeasure,
        @Nullable BigDecimal quantity,
        @Nullable BigDecimal netUnitPrice,
        @Nullable BigDecimal netAmount,
        @Nullable String vatRate,
        @Nullable BigDecimal grossAmount,
        @Nullable BigDecimal vatAmount,
        @Nullable LocalDate deliveryDate,
        @Nullable String uuid,
        @Nullable String index,
        @Nullable String cnCode,
        @Nullable String pkobCode,
        @Nullable BigDecimal grossUnitPrice,
        @Nullable BigDecimal discountAmount,
        @Nullable BigDecimal exciseAmount,
        @Nullable BigDecimal exchangeRate,
        @Nullable BigDecimal valueAddedTaxRate,
        @Nullable Boolean annex15,
        @Nullable Boolean correctionStateBefore,
        @Nullable String gtuCode,
        @Nullable String procedureMarking) {

    private static final String ERR_BAD_ROW_NUMBER = "rowNumber must be >= 1";
    private static final int MIN_ROW_NUMBER = 1;

    public InvoiceLineItem {
        if (rowNumber < MIN_ROW_NUMBER) {
            throw new IllegalArgumentException(ERR_BAD_ROW_NUMBER);
        }
    }

    /**
     * Start a fluent builder for a line item. {@code rowNumber} is required;
     * every other field is optional and defaults to {@code null}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link InvoiceLineItem}. Optional fields default to
     * {@code null}; {@code rowNumber} must be set to a value {@code >= 1}.
     *
     * <p>The builder mirrors the full flat {@code FaWiersz} scalar surface
     * (ADR-035), so one field per record component is expected here.
     */
    @SuppressWarnings("PMD.TooManyFields")
    public static final class Builder {

        private int rowNumber;
        private @Nullable String description;
        private @Nullable String gtin;
        private @Nullable String pkwiu;
        private @Nullable String unitOfMeasure;
        private @Nullable BigDecimal quantity;
        private @Nullable BigDecimal netUnitPrice;
        private @Nullable BigDecimal netAmount;
        private @Nullable String vatRate;
        private @Nullable BigDecimal grossAmount;
        private @Nullable BigDecimal vatAmount;
        private @Nullable LocalDate deliveryDate;
        private @Nullable String uuid;
        private @Nullable String index;
        private @Nullable String cnCode;
        private @Nullable String pkobCode;
        private @Nullable BigDecimal grossUnitPrice;
        private @Nullable BigDecimal discountAmount;
        private @Nullable BigDecimal exciseAmount;
        private @Nullable BigDecimal exchangeRate;
        private @Nullable BigDecimal valueAddedTaxRate;
        private @Nullable Boolean annex15;
        private @Nullable Boolean correctionStateBefore;
        private @Nullable String gtuCode;
        private @Nullable String procedureMarking;

        private Builder() {
        }

        public Builder rowNumber(int value) {
            this.rowNumber = value;
            return this;
        }

        public Builder description(@Nullable String value) {
            this.description = value;
            return this;
        }

        public Builder gtin(@Nullable String value) {
            this.gtin = value;
            return this;
        }

        public Builder pkwiu(@Nullable String value) {
            this.pkwiu = value;
            return this;
        }

        public Builder unitOfMeasure(@Nullable String value) {
            this.unitOfMeasure = value;
            return this;
        }

        public Builder quantity(@Nullable BigDecimal value) {
            this.quantity = value;
            return this;
        }

        public Builder netUnitPrice(@Nullable BigDecimal value) {
            this.netUnitPrice = value;
            return this;
        }

        public Builder netAmount(@Nullable BigDecimal value) {
            this.netAmount = value;
            return this;
        }

        public Builder vatRate(@Nullable String value) {
            this.vatRate = value;
            return this;
        }

        public Builder grossAmount(@Nullable BigDecimal value) {
            this.grossAmount = value;
            return this;
        }

        public Builder vatAmount(@Nullable BigDecimal value) {
            this.vatAmount = value;
            return this;
        }

        public Builder deliveryDate(@Nullable LocalDate value) {
            this.deliveryDate = value;
            return this;
        }

        public Builder uuid(@Nullable String value) {
            this.uuid = value;
            return this;
        }

        public Builder index(@Nullable String value) {
            this.index = value;
            return this;
        }

        public Builder cnCode(@Nullable String value) {
            this.cnCode = value;
            return this;
        }

        public Builder pkobCode(@Nullable String value) {
            this.pkobCode = value;
            return this;
        }

        public Builder grossUnitPrice(@Nullable BigDecimal value) {
            this.grossUnitPrice = value;
            return this;
        }

        public Builder discountAmount(@Nullable BigDecimal value) {
            this.discountAmount = value;
            return this;
        }

        public Builder exciseAmount(@Nullable BigDecimal value) {
            this.exciseAmount = value;
            return this;
        }

        public Builder exchangeRate(@Nullable BigDecimal value) {
            this.exchangeRate = value;
            return this;
        }

        public Builder valueAddedTaxRate(@Nullable BigDecimal value) {
            this.valueAddedTaxRate = value;
            return this;
        }

        public Builder annex15(@Nullable Boolean value) {
            this.annex15 = value;
            return this;
        }

        public Builder correctionStateBefore(@Nullable Boolean value) {
            this.correctionStateBefore = value;
            return this;
        }

        public Builder gtuCode(@Nullable String value) {
            this.gtuCode = value;
            return this;
        }

        public Builder procedureMarking(@Nullable String value) {
            this.procedureMarking = value;
            return this;
        }

        public InvoiceLineItem build() {
            return new InvoiceLineItem(
                    rowNumber, description, gtin, pkwiu, unitOfMeasure, quantity,
                    netUnitPrice, netAmount, vatRate, grossAmount, vatAmount,
                    deliveryDate, uuid, index, cnCode, pkobCode, grossUnitPrice,
                    discountAmount, exciseAmount, exchangeRate, valueAddedTaxRate,
                    annex15, correctionStateBefore, gtuCode, procedureMarking);
        }
    }
}
