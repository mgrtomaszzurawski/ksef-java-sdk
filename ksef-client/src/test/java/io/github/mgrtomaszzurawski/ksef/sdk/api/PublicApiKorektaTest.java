/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Invoices;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architecture gate for REQ-OFFLINE-005 — technical correction (korekta
 * techniczna) is permitted only in an online session.
 *
 * <p>Enforced structurally: {@link OnlineSession#sendTechnicalCorrection(Invoice, byte[])}
 * is the canonical online entry-point. The batch facade
 * ({@code Invoices.submitBatch(...)}) exposes neither a method named
 * with {@code TechnicalCorrection} nor any signature accepting the hash
 * payload tuple required for korekta techniczna. Reflection scan asserts
 * that.
 *
 * <p>Spec citation: {@code ksef-docs/offline/korekta-techniczna.md} —
 * "korekta techniczna jest dostępna wyłącznie w sesji interaktywnej (online)".
 */
class PublicApiKorektaTest {

    @Test
    void onlineSession_exposesTypedTechnicalCorrectionEntryPoint() {
        boolean exposesTypedMethod = Arrays.stream(OnlineSession.class.getMethods())
                .filter(method -> method.getName().equals("sendTechnicalCorrection"))
                .anyMatch(method -> method.getParameterCount() == 2
                        && method.getParameterTypes()[0].equals(Invoice.class)
                        && method.getParameterTypes()[1].equals(byte[].class));
        assertTrue(exposesTypedMethod,
                "OnlineSession.sendTechnicalCorrection(Invoice, byte[]) must exist as the typed entry point");
    }

    @Test
    void batchSurface_exposesNoTechnicalCorrectionEntryPoint() {
        // No batch-related method named sendTechnicalCorrection on Invoices.
        boolean hasTechnicalCorrectionMethod = Arrays.stream(Invoices.class.getMethods())
                .anyMatch(method -> method.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains("technicalcorrection"));
        assertFalse(hasTechnicalCorrectionMethod,
                "Invoices (batch facade) must NOT expose a technical-correction method (REQ-OFFLINE-005)");

        // No batch method accepts the hashOfOriginal byte[] alongside Invoice — that pair
        // only flows through OnlineSession.sendTechnicalCorrection.
        List<String> batchMethodsTakingInvoiceAndHash = Arrays.stream(Invoices.class.getMethods())
                .filter(method -> method.getName().toLowerCase(java.util.Locale.ROOT).contains("batch"))
                .filter(method -> Arrays.asList(method.getParameterTypes()).contains(Invoice.class)
                        && Arrays.asList(method.getParameterTypes()).contains(byte[].class))
                .map(Method::toString)
                .collect(Collectors.toList());
        assertTrue(batchMethodsTakingInvoiceAndHash.isEmpty(),
                "Batch methods must NOT accept the (Invoice, byte[]) pair used for korekta techniczna (REQ-OFFLINE-005)");
    }
}
