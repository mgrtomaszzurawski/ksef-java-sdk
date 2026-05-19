//DEPS io.github.mgrtomaszzurawski.ksef-sdk:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Retrieve a single invoice from KSeF archive by its KSeF number,
 *   pattern-match on the typed InvoiceDocument subtype (Fa3 / Fa2 /
 *   Pef / PefKor / Unrecognized) to access schema-specific data
 *   without manually parsing XML.
 *
 * Side effects on KSeF:
 *   Read-only (GET /invoices/ksef/{ksefNumber}). Consumes one
 *   invoice-download rate-limit slot per call.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN     — pre-issued KSeF token
 *   KSEF_NIP       — taxpayer NIP (10 digits)
 *   KSEF_NUMBER    — KSeF invoice number (format YYYYMMDD-XX-...)
 *   KSEF_ENV       — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa2InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.Fa3InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PefKorInvoiceDocument;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.UnrecognizedInvoiceDocument;

public final class GetInvoiceFromArchive {

    private GetInvoiceFromArchive() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefNumber ksefNumber = KsefNumber.parse(requireEnv("KSEF_NUMBER"));
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            InvoiceDocument document = client.invoices().archive().getByKsefNumber(ksefNumber);
            describe(document);
        }
    }

    /**
     * Pattern-match on the typed document via instanceof ladder
     * (pattern-matching in switch is Java 21+, preview in 17).
     */
    private static void describe(InvoiceDocument document) {
        if (document instanceof Fa3InvoiceDocument fa3) {
            System.out.println("FA(3): P_1=" + fa3.unsafeJaxbView().getFa().getP1());
        } else if (document instanceof Fa2InvoiceDocument fa2) {
            System.out.println("FA(2): P_1=" + fa2.unsafeJaxbView().getFa().getP1());
        } else if (document instanceof PefInvoiceDocument pef) {
            System.out.println("PEF: invoiceNumber=" + pef.invoiceNumber());
        } else if (document instanceof PefKorInvoiceDocument pefKor) {
            System.out.println("PEFKOR: invoiceNumber=" + pefKor.invoiceNumber());
        } else if (document instanceof UnrecognizedInvoiceDocument unknown) {
            System.out.println("Unknown form " + unknown.formCode()
                    + " — " + unknown.xml().length + " bytes raw");
        } else {
            System.out.println("Unhandled InvoiceDocument subtype: "
                    + document.getClass().getName());
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String envName) {
        if (envName == null || envName.isBlank()) {
            return KsefEnvironment.TEST;
        }
        return switch (envName.toUpperCase()) {
            case "TEST" -> KsefEnvironment.TEST;
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
