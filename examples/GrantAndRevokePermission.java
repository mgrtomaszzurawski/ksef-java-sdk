/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Example: grant a person InvoiceRead permission, query the operation status,
 * then revoke the permission.
 *
 * Required env vars:
 *   KSEF_TOKEN  — pre-issued KSeF token
 *   KSEF_NIP    — granting taxpayer NIP (10 digits)
 *   KSEF_PESEL  — PESEL of the person receiving the permission (11 digits)
 *
 * Optional:
 *   KSEF_ENV    — TEST | DEMO | PREPROD | PROD (default: TEST)
 */
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;

public final class GrantAndRevokePermission {

    private GrantAndRevokePermission() { }

    public static void main(String[] args) {
        String token = requireEnv("KSEF_TOKEN");
        String nip = requireEnv("KSEF_NIP");
        String pesel = requireEnv("KSEF_PESEL");
        KsefEnvironment environment = resolveEnv(System.getenv("KSEF_ENV"));

        try (KsefClient client = KsefClient.builder(environment)
                .credentials(new KsefTokenCredentials(token, nip))
                .build()) {

            client.authenticate();

            PersonPermissionGrantBuilder grant = PersonPermissionGrantBuilder.forPesel(pesel)
                    .description("Grant InvoiceRead from example")
                    .personDetails("Example", "Recipient")
                    .invoiceRead();

            PermissionOperationResult granted = client.permissions().grantPerson(grant);
            System.out.println("Grant submitted, ref: " + granted.referenceNumber());

            PermissionOperationStatus status = client.permissions()
                    .getOperationStatus(granted.referenceNumber());
            System.out.println("Operation status: code=" + status.status().code()
                    + " desc=" + status.status().description());

            // Revoke right away (the operation reference doubles as the permission ID
            // once the grant is processed; in production you would query for the
            // permission ID via permissions().queryPersons(...) and pass that in).
            try {
                PermissionOperationResult revoked = client.permissions()
                        .revokeCommon(granted.referenceNumber());
                System.out.println("Revoke submitted, ref: " + revoked.referenceNumber());
            } catch (Exception revokeFailed) {
                System.out.println("Revoke skipped: " + revokeFailed.getMessage());
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
            case "PREPROD" -> KsefEnvironment.PREPROD;
            case "PROD" -> KsefEnvironment.PROD;
            default -> KsefEnvironment.custom(envName);
        };
    }
}
