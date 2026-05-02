# Examples

Copy-paste-runnable examples covering the most common KSeF SDK use cases.

Each example is a single self-contained `.java` file with a `main()` method.
They depend on the published artifact (or the local `0.1.0-SNAPSHOT`
`mvn install` you already use for `ksef-demo`).

## How to run

1. Make sure the SDK is in your local Maven repo:
   ```bash
   mvn install -pl ksef-client -DskipTests
   ```
2. Compile and run an example with the SDK on the classpath. The simplest way
   is JBang or Maven exec:
   ```bash
   # JBang (auto-resolves dependencies from //DEPS)
   jbang examples/SendOnlineInvoice.java

   # Or compile manually
   CLASSPATH=$(mvn -q dependency:build-classpath -pl ksef-client \
       -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout)
   javac -cp "$CLASSPATH:ksef-client/target/ksef-client-0.1.0-SNAPSHOT.jar" \
         examples/SendOnlineInvoice.java
   java -cp "$CLASSPATH:ksef-client/target/ksef-client-0.1.0-SNAPSHOT.jar:examples" \
        SendOnlineInvoice
   ```
3. Each example reads credentials from environment variables: `KSEF_TOKEN`,
   `KSEF_NIP`. The `SendOnlineInvoice` and `BatchInvoiceUpload` examples also
   need a path to a sample invoice XML (`KSEF_INVOICE_XML`).

## Available examples

| File | Use case |
|------|----------|
| `SendOnlineInvoice.java` | Open online session, send one invoice, retrieve the UPO. |
| `BatchInvoiceUpload.java` | Open batch session, upload pre-built parts, poll until processing completes. |
| `QueryInvoiceMetadata.java` | Date-cursor pagination across all invoices in a date range using `queryAllMetadata`. |
| `GrantAndRevokePermission.java` | Grant a person permission, query the operation status, revoke. |
| `EnrollAndRevokeCertificate.java` | Enroll a new certificate from a CSR, poll for the serial number, revoke. |
| `Handle401Refresh.java` | Demonstrate auto re-authentication on token expiry. |
| `QrCodeGeneration.java` | Generate a verification QR code for a given KSeF number. |

## Notes

- Examples use the `KsefEnvironment.TEST` URL by default. Override via
  `KSEF_ENV` env var (`TEST`, `PREPROD`, `PROD`, or any custom URL — the demo
  environment is `https://api-demo.ksef.mf.gov.pl`).
- Examples log via SLF4J. The default backend is `slf4j-simple` if your
  classpath has it; otherwise no output. See the root README's "Logging"
  section for backend setup.
- Examples are intentionally minimal — they don't handle every error path or
  use a logging framework. They are starting points, not production
  templates. For a full integration harness see [`ksef-demo/`](../ksef-demo/).
