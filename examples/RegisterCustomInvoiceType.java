//DEPS io.github.mgrtomaszzurawski:ksef-client:0.1.0-preview
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example for the unofficial KSeF SDK (preview).
 * Not affiliated with Ministerstwo Finansow or CIRFMF.
 * API may change between 0.x releases; AGPL-3.0 warranty
 * disclaimer applies. For production use the official SDK:
 * https://github.com/CIRFMF/ksef-client-java
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Register a custom InvoiceDocument type with KsefInvoiceTypes — the
 *   read-side registry the SDK uses to construct typed wrappers for
 *   FormCodes outside the built-in FA2/FA3/PEF/PEF_KOR set.
 *
 *   After registration, every read-side accessor that routes through
 *   the SDK's InvoiceDocumentConstructor (archive().getByKsefNumber,
 *   sync().asStream, session.complete().cleared(...)) returns instances
 *   of your custom class for the registered FormCode — no
 *   UnrecognizedInvoiceDocument fallback.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN        — pre-issued KSeF token
 *   KSEF_NIP          — taxpayer NIP (10 digits)
 *   KSEF_ENV          — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefInvoiceTypes;
import io.github.mgrtomaszzurawski.ksef.sdk.config.credentials.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.document.InvoiceDocument;
import java.util.Objects;

public final class RegisterCustomInvoiceType {

    private static final byte[] EMPTY_XSD = new byte[0];

    private RegisterCustomInvoiceType() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        KsefInvoiceTypes invoiceTypes = KsefInvoiceTypes.builder()
                .register(MyCustomInvoice.class)
                .build();

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .invoiceTypes(invoiceTypes)
                .build()) {
            System.out.println("Custom invoice types registered: " + invoiceTypes);
            // Subsequent reads of MY-1 invoices return MyCustomInvoice
            // instances rather than UnrecognizedInvoiceDocument.
        }
    }

    /** Convention required by KsefInvoiceTypes.builder().register: */
    public static final class MyCustomInvoice implements InvoiceDocument {

        public static final FormCode FORM_CODE = FormCode.custom("MY-1", "1-0", "MyInvoice", EMPTY_XSD);

        private final byte[] xml;

        private MyCustomInvoice(byte[] xml) {
            this.xml = xml.clone();
        }

        public static MyCustomInvoice from(byte[] xml) {
            return new MyCustomInvoice(Objects.requireNonNull(xml, "xml"));
        }

        @Override
        public FormCode formCode() {
            return FORM_CODE;
        }

        @Override
        public byte[] xml() {
            return xml.clone();
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " environment variable is required");
        }
        return value;
    }

    private static KsefEnvironment resolveEnv(String name) {
        if (name == null || name.isBlank() || "TEST".equalsIgnoreCase(name)) {
            return KsefEnvironment.TEST;
        }
        return switch (name.toUpperCase()) {
            case "DEMO" -> KsefEnvironment.DEMO;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.TEST;
        };
    }
}
