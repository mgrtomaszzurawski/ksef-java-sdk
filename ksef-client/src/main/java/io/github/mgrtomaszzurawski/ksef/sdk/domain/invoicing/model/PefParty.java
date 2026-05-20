/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;


import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * UBL party record (AccountingSupplierParty / AccountingCustomerParty)
 * shared between the PEF(3) Invoice and PEF_KOR(3) CreditNote
 * builders.
 *
 * @param endpointId Peppol endpoint identifier — usually the party's
 *     NIP / VAT number — populates {@code cbc:EndpointID}
 * @param endpointSchemeId scheme code for {@link #endpointId()} —
 *     defaults to {@code "0192"} (Poland NIP) when null
 * @param registrationName party registered name
 *     (cac:PartyName/cbc:Name)
 * @param taxId Polish tax identifier (NIP) — emitted as
 *     {@code cac:PartyTaxScheme/cbc:CompanyID}
 * @param address postal address
 *
 * @since 0.1.0
 */
public record PefParty(
        String endpointId,
        @Nullable String endpointSchemeId,
        String registrationName,
        String taxId,
        PefAddress address) {

    private static final String ERR_NULL_ENDPOINT = "endpointId must not be null";
    private static final String ERR_NULL_NAME = "registrationName must not be null";
    private static final String ERR_NULL_TAX_ID = "taxId must not be null";
    private static final String ERR_NULL_ADDRESS = "address must not be null";

    /** Default scheme identifier for Polish NIP endpoints. */
    public static final String DEFAULT_ENDPOINT_SCHEME_ID = "0192";

    public PefParty {
        Objects.requireNonNull(endpointId, ERR_NULL_ENDPOINT);
        Objects.requireNonNull(registrationName, ERR_NULL_NAME);
        Objects.requireNonNull(taxId, ERR_NULL_TAX_ID);
        Objects.requireNonNull(address, ERR_NULL_ADDRESS);
    }

    /** Resolve {@link #endpointSchemeId()} with the Polish-NIP default. */
    public String resolvedEndpointSchemeId() {
        return endpointSchemeId != null ? endpointSchemeId : DEFAULT_ENDPOINT_SCHEME_ID;
    }
}
