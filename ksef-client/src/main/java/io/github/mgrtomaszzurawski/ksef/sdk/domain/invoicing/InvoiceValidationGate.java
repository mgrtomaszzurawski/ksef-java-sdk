/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing;

import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.Severity;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefXmlValidator.ValidationIssue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Internal pre-flight gate that runs {@link KsefXmlValidator#validate}
 * on outgoing invoice XML for all known {@link FormCode} values
 * (FA(2), FA(3), PEF(3), PEF_KOR(3)). Skips validation for
 * {@link FormCode#custom} entries — the SDK has no XSD bundled for
 * those, so the local validator is unable to make a meaningful claim.
 *
 * <p>Throws {@link KsefXmlValidator.KsefXmlValidationException} when
 * any issue with severity {@link Severity#ERROR} or
 * {@link Severity#FATAL} is detected. Warnings are silently allowed
 * through.
 *
 * @since 1.0.0
 */
final class InvoiceValidationGate {

    private static final Set<FormCode> KNOWN_FORM_CODES =
            Set.of(FormCode.FA2, FormCode.FA3, FormCode.PEF3, FormCode.PEF_KOR3);

    private InvoiceValidationGate() {
    }

    /**
     * Validate {@code invoiceXml} against the bundled XSD for {@code formCode}.
     * Custom form codes (no bundled XSD) are skipped. Throws
     * {@link KsefXmlValidator.KsefXmlValidationException} on hard errors.
     */
    static void validate(FormCode formCode, byte[] invoiceXml) {
        if (!KNOWN_FORM_CODES.contains(formCode)) {
            return;
        }
        List<ValidationIssue> issues = KsefXmlValidator.validate(invoiceXml, formCode);
        if (issues.isEmpty()) {
            return;
        }
        boolean hasFailure = false;
        List<String> failures = new ArrayList<>(issues.size());
        for (ValidationIssue issue : issues) {
            failures.add(issue.toString());
            if (issue.severity() == Severity.ERROR || issue.severity() == Severity.FATAL) {
                hasFailure = true;
            }
        }
        if (hasFailure) {
            throw new KsefXmlValidator.KsefXmlValidationException(
                    "Invoice XML failed XSD validation: " + String.join("; ", failures),
                    List.copyOf(failures));
        }
    }
}
