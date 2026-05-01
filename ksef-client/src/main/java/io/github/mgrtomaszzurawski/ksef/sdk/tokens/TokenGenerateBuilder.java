/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.tokens;

import io.github.mgrtomaszzurawski.ksef.client.model.GenerateTokenRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TokenPermissionTypeRaw;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for KSeF API token generation requests.
 * <p>
 * Required: description (5-256 chars), at least one permission.
 * <p>
 * Usage:
 * <pre>{@code
 * GenerateTokenRequestRaw request = TokenGenerateBuilder.create("Invoice reader token")
 *     .invoiceRead()
 *     .credentialsRead()
 *     .build();
 * }</pre>
 */
public final class TokenGenerateBuilder {

    private static final int DESCRIPTION_MIN_LENGTH = 5;
    private static final int DESCRIPTION_MAX_LENGTH = 256;
    private static final String ERR_DESCRIPTION_LENGTH = "description must be between 5 and 256 characters";
    private static final String ERR_PERMISSIONS_EMPTY = "at least one permission is required";
    private static final String ERR_NULL_DESCRIPTION = "description is required";

    private final String description;
    private final List<TokenPermissionTypeRaw> permissions = new ArrayList<>();

    private TokenGenerateBuilder(String description) {
        this.description = Objects.requireNonNull(description, ERR_NULL_DESCRIPTION);
    }

    /**
     * Create a builder with the required description.
     *
     * @param description token description (5-256 characters)
     */
    public static TokenGenerateBuilder create(String description) {
        return new TokenGenerateBuilder(description);
    }

    public TokenGenerateBuilder invoiceRead() {
        permissions.add(TokenPermissionTypeRaw.INVOICE_READ);
        return this;
    }

    public TokenGenerateBuilder invoiceWrite() {
        permissions.add(TokenPermissionTypeRaw.INVOICE_WRITE);
        return this;
    }

    public TokenGenerateBuilder credentialsRead() {
        permissions.add(TokenPermissionTypeRaw.CREDENTIALS_READ);
        return this;
    }

    public TokenGenerateBuilder credentialsManage() {
        permissions.add(TokenPermissionTypeRaw.CREDENTIALS_MANAGE);
        return this;
    }

    public TokenGenerateBuilder subunitManage() {
        permissions.add(TokenPermissionTypeRaw.SUBUNIT_MANAGE);
        return this;
    }

    public TokenGenerateBuilder enforcementOperations() {
        permissions.add(TokenPermissionTypeRaw.ENFORCEMENT_OPERATIONS);
        return this;
    }

    public TokenGenerateBuilder introspection() {
        permissions.add(TokenPermissionTypeRaw.INTROSPECTION);
        return this;
    }

    /**
     * Build the token generation request. Validates description length and permissions.
     *
     * @return the request ready to pass to {@code TokenClient.generate()}
     * @throws IllegalStateException if validation fails
     */
    public GenerateTokenRequestRaw build() {
        if (description.length() < DESCRIPTION_MIN_LENGTH || description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalStateException(ERR_DESCRIPTION_LENGTH);
        }
        if (permissions.isEmpty()) {
            throw new IllegalStateException(ERR_PERMISSIONS_EMPTY);
        }

        GenerateTokenRequestRaw request = new GenerateTokenRequestRaw();
        request.setDescription(description);
        request.setPermissions(new ArrayList<>(permissions));
        return request;
    }
}
