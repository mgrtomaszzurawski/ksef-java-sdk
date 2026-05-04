/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.SendInvoiceCommand;
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
 * <p>Enforced structurally: {@link KsefSession#send(SendInvoiceCommand)}
 * accepts {@link SendInvoiceCommand.TechnicalCorrection} and the
 * convenience overload {@link KsefSession#sendTechnicalCorrection(byte[], byte[])}
 * exists. {@link KsefBatchSession} exposes neither — there is no public
 * method that accepts a {@code SendInvoiceCommand} or a corrected-invoice
 * hash. Reflection scan asserts that.
 *
 * <p>Spec citation: {@code ksef-docs/offline/korekta-techniczna.md} —
 * "korekta techniczna jest dostępna wyłącznie w sesji interaktywnej (online)".
 */
class PublicApiKorektaTest {

    @Test
    void onlineSession_exposesTechnicalCorrectionEntryPoints() {
        boolean acceptsCommand = Arrays.stream(KsefSession.class.getMethods())
                .filter(method -> method.getName().equals("send"))
                .anyMatch(method -> method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(SendInvoiceCommand.class));
        assertTrue(acceptsCommand,
                "KsefSession.send(SendInvoiceCommand) must exist for technical-correction dispatch");

        boolean exposesConvenienceMethod = Arrays.stream(KsefSession.class.getMethods())
                .anyMatch(method -> method.getName().equals("sendTechnicalCorrection"));
        assertTrue(exposesConvenienceMethod,
                "KsefSession.sendTechnicalCorrection must exist as convenience overload");
    }

    @Test
    void batchSession_exposesNoTechnicalCorrectionEntryPoint() {
        // No method named sendTechnicalCorrection
        boolean hasTechnicalCorrectionMethod = Arrays.stream(KsefBatchSession.class.getMethods())
                .anyMatch(method -> method.getName().toLowerCase(java.util.Locale.ROOT).contains("technicalcorrection"));
        assertFalse(hasTechnicalCorrectionMethod,
                "KsefBatchSession must NOT expose a technical-correction method (REQ-OFFLINE-005)");

        // No method takes SendInvoiceCommand or a 32-byte hash parameter named
        // hashOfCorrectedInvoice — batch sessions cannot pipe per-invoice corrections.
        List<String> sendCommandMethods = Arrays.stream(KsefBatchSession.class.getMethods())
                .filter(method -> method.getName().equals("send"))
                .filter(method -> method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(SendInvoiceCommand.class))
                .map(Method::toString)
                .collect(Collectors.toList());
        assertTrue(sendCommandMethods.isEmpty(),
                "KsefBatchSession must NOT accept SendInvoiceCommand as a parameter (REQ-OFFLINE-005)");
    }
}
