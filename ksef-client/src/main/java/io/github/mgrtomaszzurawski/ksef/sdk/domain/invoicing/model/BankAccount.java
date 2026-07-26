/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A bank account listed under {@code Fa/Platnosc} — a seller settlement
 * account ({@code RachunekBankowy}) or a factor account
 * ({@code RachunekBankowyFaktora}). Maps onto the {@code TRachunekBankowy}
 * XSD type.
 *
 * @param accountNumber full account number ({@code NrRB}) — mandatory
 * @param swift SWIFT/BIC code ({@code SWIFT}) — null when domestic
 * @param ownAccountType split-payment own-account code
 *     ({@code RachunekWlasnyBanku}: 1 = purchased-receivables account,
 *     2 = collection-and-transfer account); null when not a bank's own account
 * @param bankName bank name ({@code NazwaBanku}) — null when not supplied
 * @param description free-text account description ({@code OpisRachunku})
 *
 * @since 0.1.0
 */
public record BankAccount(
        String accountNumber,
        @Nullable String swift,
        @Nullable Integer ownAccountType,
        @Nullable String bankName,
        @Nullable String description) {

    private static final String ERR_NULL_ACCOUNT = "accountNumber must not be null";

    public BankAccount {
        Objects.requireNonNull(accountNumber, ERR_NULL_ACCOUNT);
    }
}
