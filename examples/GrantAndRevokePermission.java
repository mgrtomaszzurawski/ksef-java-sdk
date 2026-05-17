//DEPS io.github.mgrtomaszzurawski:ksef-client:1.0.0
//DEPS org.slf4j:slf4j-simple:2.0.16

/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Reference code (not a runnable script): adapt to your application.
 *
 * What this shows:
 *   Grant a person InvoiceRead permission, query the operation status, then
 *   revoke the permission.
 *
 * Side effects on KSeF:
 *   Modifies entity permissions on the granting taxpayer's account.
 *
 * Inputs the snippet expects (read from env vars when run as-is):
 *   KSEF_TOKEN  — pre-issued KSeF token
 *   KSEF_NIP    — granting taxpayer NIP (10 digits)
 *   KSEF_PESEL  — PESEL of the person receiving the permission (11 digits)
 *   KSEF_ENV    — TEST | DEMO | PROD (optional, default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;

public final class GrantAndRevokePermission {

    private GrantAndRevokePermission() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        String pesel = requireEnv("KSEF_PESEL");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder().environment(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            // Authentication is lazy — the first authenticated call triggers it.
            PersonPermissionGrantBuilder grant = PersonPermissionGrantBuilder.forPesel(pesel)
                    .description("Grant InvoiceRead from example")
                    .personDetails("Example", "Recipient")
                    .invoiceRead();

            PermissionOperationStatus granted = client.permissions().grantPerson(grant.build());
            System.out.println("Grant completed: code=" + granted.status().code()
                    + " desc=" + granted.status().description());

            // In production look up the permission ID via permissions().queryPersons(...)
            // and pass that in. Demo flow doesn't have a stable ID without a query, so
            // we skip revoke unless the caller passes KSEF_PERMISSION_ID.
            String permissionId = System.getenv("KSEF_PERMISSION_ID");
            if (permissionId != null && !permissionId.isBlank()) {
                try {
                    PermissionOperationStatus revoked = client.permissions().revokePermission(permissionId);
                    System.out.println("Revoke completed: code=" + revoked.status().code()
                            + " desc=" + revoked.status().description());
                } catch (Exception revokeFailed) {
                    System.out.println("Revoke skipped: " + revokeFailed.getMessage());
                }
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
