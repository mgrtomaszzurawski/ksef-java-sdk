/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
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
 * <p>Enforced structurally: {@link OnlineSession#send(SendInvoiceCommand)}
 * accepts {@link SendInvoiceCommand.TechnicalCorrection} and the
 * convenience overload {@link OnlineSession#sendTechnicalCorrection(byte[], byte[])}
 * exists. The batch facade ({@code Invoices.submitBatch(...)}) exposes
 * neither — there is no public method that accepts a {@code SendInvoiceCommand}
 * or a corrected-invoice hash on the batch path. Reflection scan asserts that.
 *
 * <p>Spec citation: {@code ksef-docs/offline/korekta-techniczna.md} —
 * "korekta techniczna jest dostępna wyłącznie w sesji interaktywnej (online)".
 */
class PublicApiKorektaTest {

    @Test
    void onlineSession_exposesTechnicalCorrectionEntryPoints() {
        boolean acceptsCommand = Arrays.stream(OnlineSession.class.getMethods())
                .filter(method -> method.getName().equals("send"))
                .anyMatch(method -> method.getParameterCount() == 1
                        && method.getParameterTypes()[0].equals(SendInvoiceCommand.class));
        assertTrue(acceptsCommand,
                "OnlineSession.send(SendInvoiceCommand) must exist for technical-correction dispatch");

        boolean exposesConvenienceMethod = Arrays.stream(OnlineSession.class.getMethods())
                .anyMatch(method -> method.getName().equals("sendTechnicalCorrection"));
        assertTrue(exposesConvenienceMethod,
                "OnlineSession.sendTechnicalCorrection must exist as convenience overload");
    }

    @Test
    void batchSurface_exposesNoTechnicalCorrectionEntryPoint() {
        // No batch-related method named sendTechnicalCorrection on InvoiceClient.
        boolean hasTechnicalCorrectionMethod = Arrays.stream(InvoiceClient.class.getMethods())
                .anyMatch(method -> method.getName().toLowerCase(java.util.Locale.ROOT)
                        .contains("technicalcorrection"));
        assertFalse(hasTechnicalCorrectionMethod,
                "InvoiceClient (batch facade) must NOT expose a technical-correction method (REQ-OFFLINE-005)");

        // No batch method accepts SendInvoiceCommand — that type only flows through OnlineSession.
        List<String> batchMethodsTakingCommand = Arrays.stream(InvoiceClient.class.getMethods())
                .filter(method -> method.getName().toLowerCase(java.util.Locale.ROOT).contains("batch"))
                .filter(method -> Arrays.stream(method.getParameterTypes())
                        .anyMatch(parameterType -> parameterType.equals(SendInvoiceCommand.class)))
                .map(Method::toString)
                .collect(Collectors.toList());
        assertTrue(batchMethodsTakingCommand.isEmpty(),
                "Batch methods must NOT accept SendInvoiceCommand as a parameter (REQ-OFFLINE-005)");
    }
}
