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
 * Per-request validation failure (HTTP 400) carrying every server-side
 * error in a typed {@link #errors() list} so the consumer can react
 * without parsing {@code responseBody} JSON manually.
 *
 * <p>Use case: the consumer built a request body that the KSeF server
 * rejected on per-field validation (most common code: 21405 — invalid
 * field shape) or JSON parsing (21001). The exception lists every
 * field-level problem in one shot — KSeF reports all errors per pass,
 * not just the first.
 *
 * <p>Example:
 * <pre>{@code
 * try {
 *     client.invoices().send(invoiceXml);
 * } catch (KsefValidationException ex) {
 *     for (var err : ex.errors()) {
 *         System.err.println(err.code() + ": " + err.description());
 *         err.details().forEach(d -> System.err.println("  • " + d));
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public class KsefValidationException extends KsefException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<KsefValidationError> errors;

    public KsefValidationException(String message, @Nullable Throwable cause, int statusCode,
                                   @Nullable String responseBody, List<KsefValidationError> errors) {
        super(message, cause, statusCode, responseBody);
        Objects.requireNonNull(errors, "errors must not be null");
        this.errors = List.copyOf(errors);
    }

    /**
     * The structured list of validation errors as returned by the server.
     * Empty when the wire body could not be parsed; always non-null.
     */
    public List<KsefValidationError> errors() {
        return errors;
    }

    /**
     * Override that returns the first parsed error's KSeF-internal code in
     * O(1) — the {@link #errors() errors list} was populated once at
     * construction by the factory, so consumers can branch on
     * {@code ex.exceptionCode()} without re-parsing {@link #responseBody()}.
     */
    @Override
    public @Nullable Integer exceptionCode() {
        return errors.isEmpty() ? null : errors.get(0).code();
    }
}
