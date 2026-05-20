/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.exception;

import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Thrown when a KSeF online or batch session reaches a terminal status code
 * other than {@code 200}. Surfaces the server-reported processing failure
 * (e.g. invoice batch validation rejected, schema mismatch, fiscal-rule
 * breach) instead of letting {@code close()} return silently.
 *
 * <p>Carries the session reference number, the terminal status code, the
 * server-supplied description, and any nested error details so callers can
 * map the failure to a typed business outcome.
 *
 * @since 0.1.0
 */
public class KsefSessionTerminalFailureException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE_TEMPLATE =
            "Session %s reached terminal failure state: code=%d description=%s";

    private final String referenceNumber;
    private final int code;
    private final @Nullable String description;
    private final List<String> details;

    public KsefSessionTerminalFailureException(String referenceNumber, int code,
                                               @Nullable String description, @Nullable List<String> details) {
        super(formatMessage(referenceNumber, code, description), code, null);
        this.referenceNumber = Objects.requireNonNull(referenceNumber, "referenceNumber");
        this.code = code;
        this.description = description;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String referenceNumber() {
        return referenceNumber;
    }

    public int code() {
        return code;
    }

    public @Nullable String description() {
        return description;
    }

    public List<String> details() {
        return details;
    }

    private static String formatMessage(String referenceNumber, int code, @Nullable String description) {
        return String.format(MESSAGE_TEMPLATE, referenceNumber, code, description);
    }
}
