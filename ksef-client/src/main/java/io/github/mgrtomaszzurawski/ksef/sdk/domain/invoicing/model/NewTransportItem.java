/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One new means of transport reported on an intra-Community supply — the
 * typed view of a {@code Fa/Adnotacje/NoweSrodkiTransportu/NowySrodekTransportu}
 * element (art. 42 ust. 5, art. 2 pkt 10 of the VAT Act). Every scalar the
 * FA(2)/FA(3) schema declares on the item is surfaced so the read path never
 * forces the {@code unsafeJaxbView()} escape hatch for a transport field.
 *
 * <p>The XSD makes {@link #admissionDate()} ({@code P_22A}) and
 * {@link #invoiceLineNumber()} ({@code P_NrWierszaNST}) mandatory; the
 * common descriptive fields ({@code P_22BMK}..{@code P_22BRP}) are optional.
 * The remaining fields form an XSD choice on the vehicle category: a land
 * vehicle carries {@link #mileage()} ({@code P_22B}) plus at most one of the
 * VIN / body / chassis / frame identifiers and a {@link #vehicleType()}; a
 * vessel carries {@link #vesselWorkingHours()} ({@code P_22C}) and an optional
 * {@link #hullNumber()}; an aircraft carries {@link #aircraftWorkingHours()}
 * ({@code P_22D}) and an optional {@link #factoryNumber()}. Fields outside the
 * selected category return {@code null}. Construct via {@link #builder()} — the
 * canonical constructor exists for internal mapping.
 *
 * @param admissionDate date the means of transport was first put into service ({@code P_22A})
 * @param invoiceLineNumber invoice line where the supply is itemised ({@code P_NrWierszaNST}) — 1-based
 * @param make make of the means of transport ({@code P_22BMK}) — may be null
 * @param model model of the means of transport ({@code P_22BMD}) — may be null
 * @param color colour of the means of transport ({@code P_22BK}) — may be null
 * @param registrationNumber registration number ({@code P_22BNR}) — may be null
 * @param productionYear year of production ({@code P_22BRP}) — may be null
 * @param mileage odometer reading for a land vehicle ({@code P_22B}) — may be null
 * @param vin vehicle identification number ({@code P_22B1}) — may be null
 * @param bodyNumber body number ({@code P_22B2}) — may be null
 * @param chassisNumber chassis number ({@code P_22B3}) — may be null
 * @param frameNumber frame number ({@code P_22B4}) — may be null
 * @param vehicleType type of the land vehicle ({@code P_22BT}) — may be null
 * @param vesselWorkingHours working hours for a vessel ({@code P_22C}) — may be null
 * @param hullNumber hull number of the vessel ({@code P_22C1}) — may be null
 * @param aircraftWorkingHours working hours for an aircraft ({@code P_22D}) — may be null
 * @param factoryNumber factory number of the aircraft ({@code P_22D1}) — may be null
 *
 * @since 0.1.0
 */
public record NewTransportItem(
        LocalDate admissionDate,
        int invoiceLineNumber,
        @Nullable String make,
        @Nullable String model,
        @Nullable String color,
        @Nullable String registrationNumber,
        @Nullable String productionYear,
        @Nullable String mileage,
        @Nullable String vin,
        @Nullable String bodyNumber,
        @Nullable String chassisNumber,
        @Nullable String frameNumber,
        @Nullable String vehicleType,
        @Nullable String vesselWorkingHours,
        @Nullable String hullNumber,
        @Nullable String aircraftWorkingHours,
        @Nullable String factoryNumber) {

    private static final String ERR_NULL_DATE = "admissionDate must not be null";
    private static final String ERR_BAD_LINE_NUMBER = "invoiceLineNumber must be >= 1";
    private static final int MIN_LINE_NUMBER = 1;

    public NewTransportItem {
        Objects.requireNonNull(admissionDate, ERR_NULL_DATE);
        if (invoiceLineNumber < MIN_LINE_NUMBER) {
            throw new IllegalArgumentException(ERR_BAD_LINE_NUMBER);
        }
    }

    /**
     * Start a fluent builder for a new means of transport. {@code admissionDate}
     * and {@code invoiceLineNumber} are required; every other field is optional
     * and defaults to {@code null}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link NewTransportItem}. Optional fields default to
     * {@code null}; {@code admissionDate} must be set and {@code invoiceLineNumber}
     * must be {@code >= 1}.
     *
     * <p>The builder mirrors the full flat {@code NowySrodekTransportu} scalar
     * surface (ADR-035), so one field per record component is expected here.
     */
    @SuppressWarnings("PMD.TooManyFields")
    public static final class Builder {

        private @Nullable LocalDate admissionDate;
        private int invoiceLineNumber;
        private @Nullable String make;
        private @Nullable String model;
        private @Nullable String color;
        private @Nullable String registrationNumber;
        private @Nullable String productionYear;
        private @Nullable String mileage;
        private @Nullable String vin;
        private @Nullable String bodyNumber;
        private @Nullable String chassisNumber;
        private @Nullable String frameNumber;
        private @Nullable String vehicleType;
        private @Nullable String vesselWorkingHours;
        private @Nullable String hullNumber;
        private @Nullable String aircraftWorkingHours;
        private @Nullable String factoryNumber;

        private Builder() {
        }

        public Builder admissionDate(LocalDate value) {
            this.admissionDate = value;
            return this;
        }

        public Builder invoiceLineNumber(int value) {
            this.invoiceLineNumber = value;
            return this;
        }

        public Builder make(@Nullable String value) {
            this.make = value;
            return this;
        }

        public Builder model(@Nullable String value) {
            this.model = value;
            return this;
        }

        public Builder color(@Nullable String value) {
            this.color = value;
            return this;
        }

        public Builder registrationNumber(@Nullable String value) {
            this.registrationNumber = value;
            return this;
        }

        public Builder productionYear(@Nullable String value) {
            this.productionYear = value;
            return this;
        }

        public Builder mileage(@Nullable String value) {
            this.mileage = value;
            return this;
        }

        public Builder vin(@Nullable String value) {
            this.vin = value;
            return this;
        }

        public Builder bodyNumber(@Nullable String value) {
            this.bodyNumber = value;
            return this;
        }

        public Builder chassisNumber(@Nullable String value) {
            this.chassisNumber = value;
            return this;
        }

        public Builder frameNumber(@Nullable String value) {
            this.frameNumber = value;
            return this;
        }

        public Builder vehicleType(@Nullable String value) {
            this.vehicleType = value;
            return this;
        }

        public Builder vesselWorkingHours(@Nullable String value) {
            this.vesselWorkingHours = value;
            return this;
        }

        public Builder hullNumber(@Nullable String value) {
            this.hullNumber = value;
            return this;
        }

        public Builder aircraftWorkingHours(@Nullable String value) {
            this.aircraftWorkingHours = value;
            return this;
        }

        public Builder factoryNumber(@Nullable String value) {
            this.factoryNumber = value;
            return this;
        }

        public NewTransportItem build() {
            LocalDate date = Objects.requireNonNull(admissionDate, ERR_NULL_DATE);
            return new NewTransportItem(
                    date, invoiceLineNumber, make, model, color,
                    registrationNumber, productionYear, mileage, vin, bodyNumber,
                    chassisNumber, frameNumber, vehicleType, vesselWorkingHours,
                    hullNumber, aircraftWorkingHours, factoryNumber);
        }
    }
}
