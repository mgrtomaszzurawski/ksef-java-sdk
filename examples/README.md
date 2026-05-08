# Examples

These are **reference code, not runnable scripts** — they show how each
SDK feature is wired. Adapt them to your application context. Do not run
them blindly against PROD; several examples have destructive side effects
on KSeF (filing real legally-binding invoices, granting/revoking
permissions, burning cert quota).

Each example is a single self-contained `.java` file with a `main()`
method. The header docstring on every file lists *what it shows*, the
*side effects on KSeF*, and the *inputs the snippet expects* when run
as-is.

## Available examples

| File | Use case |
|------|----------|
| `SendOnlineInvoice.java` | Open online session, send one invoice, retrieve the UPO. |
| `BatchInvoiceUpload.java` | Open batch session, upload pre-built parts, poll until processing completes. |
| `QueryInvoiceMetadata.java` | Lazy paginator across all invoices in a date range using `streamInvoicesByMetadata`. |
| `GrantAndRevokePermission.java` | Grant a person permission, query the operation status, revoke. |
| `EnrollAndRevokeCertificate.java` | Enroll a new certificate from a CSR, poll for the serial number, revoke. |
| `Handle401Refresh.java` | Demonstrate auto re-authentication on token expiry. |
| `QrCodeGeneration.java` | KOD I — generate online-invoice verification QR (SHA-256-only, no signing). |
| `QrCertificateGeneration.java` | KOD II — generate offline-certificate verification QR; loads PKCS#12 keystore, signs with PrivateKey (RSA-PSS / ECDSA-P1363 auto-detected per ADR-019). |
| `IncrementalSync.java` | Incremental invoice sync via `InvoiceSyncClient` with checkpoint persistence. |

## Notes

- Examples target `KsefEnvironment.TEST` by default. Override via
  `KSEF_ENV` (`TEST`, `DEMO`, `PROD`, or any custom URL — the demo
  environment is `https://api-demo.ksef.mf.gov.pl`).
- Examples log via SLF4J. The default backend is `slf4j-simple` if your
  classpath has it; otherwise no output. See the root README's "Logging"
  section for backend setup.
- Examples are intentionally minimal — they don't handle every error path
  or use a logging framework. They are starting points, not production
  templates. For a full integration harness see [`ksef-demo/`](../ksef-demo/).
- The `ksef-examples` Maven module compile-checks every file against the
  published API to prevent silent drift; running examples is up to you.
