//DEPS io.github.mgrtomaszzurawski.ksef-sdk:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Query KSeF limits — context-level (online/batch session caps),
 *   subject-level (certificate + enrollment monthly quotas), and
 *   rate limits (per-operation sliding windows). Read-only
 *   diagnostic; the SDK does NOT proactively pace calls to stay
 *   under these limits.
 *
 * Side effects on KSeF:
 *   Read-only.
 *
 * Inputs (env vars):
 *   KSEF_TOKEN — pre-issued KSeF token
 *   KSEF_NIP   — taxpayer NIP
 *   KSEF_ENV   — TEST | DEMO | PROD (optional)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ApiRateLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.ContextLimits;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.limits.model.SubjectLimits;

public final class QueryRateLimitsAndQuotas {

    private QueryRateLimitsAndQuotas() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            ContextLimits context = client.limits().getContextLimits();
            if (context.onlineSession() != null) {
                System.out.println("Online session: max " + context.onlineSession().maxInvoices()
                        + " invoices, " + context.onlineSession().maxInvoiceSizeInMB() + " MB max");
            }
            if (context.batchSession() != null) {
                System.out.println("Batch session: max " + context.batchSession().maxInvoices()
                        + " invoices, " + context.batchSession().maxInvoiceSizeInMB() + " MB max");
            }

            SubjectLimits subject = client.limits().getSubjectLimits();
            System.out.println("Cert enrollments (monthly): " + subject.maxEnrollments());
            System.out.println("Active certs cap: " + subject.maxCertificates());

            ApiRateLimits rates = client.limits().getRateLimits();
            if (rates.invoiceSend() != null) {
                System.out.println("invoiceSend rate: "
                        + rates.invoiceSend().perSecond() + "/s, "
                        + rates.invoiceSend().perMinute() + "/min, "
                        + rates.invoiceSend().perHour() + "/h");
            }
            if (rates.invoiceStatus() != null) {
                System.out.println("invoiceStatus rate: "
                        + rates.invoiceStatus().perSecond() + "/s, "
                        + rates.invoiceStatus().perMinute() + "/min, "
                        + rates.invoiceStatus().perHour() + "/h");
            }
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
