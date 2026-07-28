/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * One line of an advance-payment order — the typed view of a
 * {@code Fa/Zamowienie/ZamowienieWiersz} element. Mirrors the flat scalar
 * surface the FA(2)/FA(3) schema declares on {@code ZamowienieWiersz}
 * (the order-side counterpart of {@code FaWiersz}); every scalar is
 * surfaced so the read path never forces the {@code unsafeJaxbView()}
 * escape hatch for an order-line field.
 *
 * <p>Only {@link #rowNumber()} ({@code NrWierszaZam}) is mandatory in the
 * XSD; every other scalar is {@code minOccurs="0"} and returns
 * {@code null} when absent. Construct via {@link #builder()} — the
 * canonical constructor exists for internal mapping.
 *
 * @param rowNumber row ordinal — 1-based, {@code NrWierszaZam}
 * @param uuid universal unique order-line id ({@code UU_IDZ}) — may be null
 * @param description product / service name ({@code P_7Z}) — may be null
 * @param index internal product index ({@code IndeksZ}) — may be null
 * @param gtin GTIN / barcode ({@code GTINZ}) — may be null
 * @param pkwiu PKWiU classification ({@code PKWiUZ}) — may be null
 * @param cnCode CN customs classification ({@code CNZ}) — may be null
 * @param pkobCode PKOB building classification ({@code PKOBZ}) — may be null
 * @param unitOfMeasure unit symbol ({@code P_8AZ}) — may be null
 * @param quantity quantity ordered ({@code P_8BZ}) — may be null
 * @param netUnitPrice unit price net ({@code P_9AZ}) — may be null
 * @param netAmount value net ({@code P_11NettoZ}) — may be null
 * @param vatAmount VAT amount ({@code P_11VatZ}) — may be null
 * @param vatRate VAT rate token ({@code P_12Z}) — may be null
 * @param valueAddedTaxRate value-added-tax rate for the OSS procedure ({@code P_12Z_XII}) — may be null
 * @param annex15 {@code true} when the line covers Annex-15 goods/services ({@code P_12Z_Zal_15}, marker {@code 1}) — may be null
 * @param gtuCode GTU goods/services group marking ({@code GTUZ}) — may be null
 * @param procedureMarking special procedure marking ({@code ProceduraZ}) — may be null
 * @param exciseAmount excise amount included in the price ({@code KwotaAkcyzyZ}) — may be null
 * @param correctionStateBefore {@code true} when the line represents the pre-correction state ({@code StanPrzedZ}, marker {@code 1}) — may be null
 *
 * @since 0.1.0
 */
public record OrderLine(
        int rowNumber,
        @Nullable String uuid,
        @Nullable String description,
        @Nullable String index,
        @Nullable String gtin,
        @Nullable String pkwiu,
        @Nullable String cnCode,
        @Nullable String pkobCode,
        @Nullable String unitOfMeasure,
        @Nullable BigDecimal quantity,
        @Nullable BigDecimal netUnitPrice,
        @Nullable BigDecimal netAmount,
        @Nullable BigDecimal vatAmount,
        @Nullable String vatRate,
        @Nullable BigDecimal valueAddedTaxRate,
        @Nullable Boolean annex15,
        @Nullable String gtuCode,
        @Nullable String procedureMarking,
        @Nullable BigDecimal exciseAmount,
        @Nullable Boolean correctionStateBefore) {

    private static final String ERR_BAD_ROW_NUMBER = "rowNumber must be >= 1";
    private static final int MIN_ROW_NUMBER = 1;

    public OrderLine {
        if (rowNumber < MIN_ROW_NUMBER) {
            throw new IllegalArgumentException(ERR_BAD_ROW_NUMBER);
        }
    }

    /**
     * Start a fluent builder for an order line. {@code rowNumber} is required;
     * every other field is optional and defaults to {@code null}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link OrderLine}. Optional fields default to
     * {@code null}; {@code rowNumber} must be set to a value {@code >= 1}.
     *
     * <p>The builder mirrors the full flat {@code ZamowienieWiersz} scalar
     * surface (ADR-035), so one field per record component is expected here.
     */
    @SuppressWarnings("PMD.TooManyFields")
    public static final class Builder {

        private int rowNumber;
        private @Nullable String uuid;
        private @Nullable String description;
        private @Nullable String index;
        private @Nullable String gtin;
        private @Nullable String pkwiu;
        private @Nullable String cnCode;
        private @Nullable String pkobCode;
        private @Nullable String unitOfMeasure;
        private @Nullable BigDecimal quantity;
        private @Nullable BigDecimal netUnitPrice;
        private @Nullable BigDecimal netAmount;
        private @Nullable BigDecimal vatAmount;
        private @Nullable String vatRate;
        private @Nullable BigDecimal valueAddedTaxRate;
        private @Nullable Boolean annex15;
        private @Nullable String gtuCode;
        private @Nullable String procedureMarking;
        private @Nullable BigDecimal exciseAmount;
        private @Nullable Boolean correctionStateBefore;

        private Builder() {
        }

        public Builder rowNumber(int value) {
            this.rowNumber = value;
            return this;
        }

        public Builder uuid(@Nullable String value) {
            this.uuid = value;
            return this;
        }

        public Builder description(@Nullable String value) {
            this.description = value;
            return this;
        }

        public Builder index(@Nullable String value) {
            this.index = value;
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

        public Builder cnCode(@Nullable String value) {
            this.cnCode = value;
            return this;
        }

        public Builder pkobCode(@Nullable String value) {
            this.pkobCode = value;
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

        public Builder vatAmount(@Nullable BigDecimal value) {
            this.vatAmount = value;
            return this;
        }

        public Builder vatRate(@Nullable String value) {
            this.vatRate = value;
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

        public Builder gtuCode(@Nullable String value) {
            this.gtuCode = value;
            return this;
        }

        public Builder procedureMarking(@Nullable String value) {
            this.procedureMarking = value;
            return this;
        }

        public Builder exciseAmount(@Nullable BigDecimal value) {
            this.exciseAmount = value;
            return this;
        }

        public Builder correctionStateBefore(@Nullable Boolean value) {
            this.correctionStateBefore = value;
            return this;
        }

        public OrderLine build() {
            return new OrderLine(
                    rowNumber, uuid, description, index, gtin, pkwiu, cnCode, pkobCode,
                    unitOfMeasure, quantity, netUnitPrice, netAmount, vatAmount, vatRate,
                    valueAddedTaxRate, annex15, gtuCode, procedureMarking, exciseAmount,
                    correctionStateBefore);
        }
    }
}
