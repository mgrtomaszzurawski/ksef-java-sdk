# ksef-client

Library module of the [KSeF Java SDK](../README.md) — coordinates
`io.github.mgrtomaszzurawski:ksef-client`.

> **Status:** Maven Central staging dry-run pending. Until the artifact is
> live on Central, install the `1.0.0` jar locally with
> `mvn install -pl ksef-client -DskipTests` to use it from a downstream
> project.

If you only need to consume the SDK, this README is enough. For project-wide
context, the architecture deep-dive, and the demo / live-validation harness,
see the [root README](../README.md) and the [`ADR/`](../ADR/) set.

## Quickstart

```xml
<dependency>
    <groupId>io.github.mgrtomaszzurawski</groupId>
    <artifactId>ksef-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;

byte[] invoiceXml = ...;

try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.TEST)
        .credentials(new KsefTokenCredentials("your-ksef-token", "1234567890"))
        .build()) {
    client.authenticate();
    try (KsefSession session = client.openSession(FormCode.FA2)) {
        session.send(invoiceXml);
    }
}
```

## Identifier types

KSeF identifies subjects (taxpayers, employees, EU entities, certificate
fingerprints) by one of four *identifier types*. Most APIs accept any of them
via `KsefIdentifier`:

| Type | Format | Use |
|------|--------|-----|
| `Nip` | 10 digits | Polish tax ID — most common authentication identifier |
| `Pesel` | 11 digits | Polish personal ID — natural-person authentication |
| `Fingerprint` | SHA-256 hex of cert public key | Certificate fingerprint identifier (foreign / EU entities) |
| `NipVatUe` | `{ownerNip}-{country}{specific}` compound | EU VAT number context bound to a Polish owner NIP |

Construct via the factory methods:

```java
KsefIdentifier.nip("1234567890");
KsefIdentifier.pesel("12345678901");
KsefIdentifier.fingerprint("abcd1234...");          // SHA-256 hex of cert pubkey
KsefIdentifier.nipVatUe("1234567890-DE123456789");  // ownerNip-{country}{specific}
```

> **VAT-UE authenticated context** is a two-step flow: (1) the EU-entity
> certificate must first be granted `EuEntityAdminPermission` by the
> Polish owner — the grant is keyed on the SHA-256 fingerprint of the
> cert's public key; (2) authentication then uses
> `CertificateSubjectIdentifier.fingerprint(hex)` together with
> `KsefIdentifier.nipVatUe(compound)` as the operation context. The
> certificate's `organizationIdentifier` RDN must be `VATPL-{ownerNip}`,
> not the compound. See `KsefCertificateCredentials` Javadoc for the
> full contract.

Some flows accept only a NIP (e.g. credentials) — those constructors take a
plain `String` for ergonomics.

## Environments

```java
KsefEnvironment.TEST                            // https://api-test.ksef.mf.gov.pl
KsefEnvironment.DEMO                            // https://api-demo.ksef.mf.gov.pl
KsefEnvironment.PROD                            // https://api.ksef.mf.gov.pl
KsefEnvironment.custom("https://...")           // self-hosted / staging
```

`PROD` is gated behind the `KsefIdentifier` validation — production NIPs only;
test NIPs throw `IllegalArgumentException`.

## Credential types

Three implementations of `KsefCredentials`:

```java
// 1. Pre-issued KSeF token (RSA-encrypted in transit during auth)
new KsefTokenCredentials("your-token", "1234567890");

// 2. PKCS#12 keystore (XAdES-BASELINE-B signing)
new KsefPkcs12Credentials(
        Path.of("keystore.p12"),
        "passphrase".toCharArray(),
        "1234567890");

// 3. Raw X.509 certificate + private key
new KsefCertificateCredentials(x509Cert, privateKey, "1234567890");
```

Pick the simplest one that matches the keys you have. All three drive the same
authentication flow under the hood.

## Builder options

```java
KsefClient.builder().environment(KsefEnvironment.TEST)
        .credentials(...)
        .connectTimeout(Duration.ofSeconds(10))    // TCP connect
        .readTimeout(Duration.ofSeconds(30))        // per-request response wait
        .retryPolicy(RetryPolicy.builder()
                .maxAttempts(3)
                .baseDelay(Duration.ofSeconds(1))
                .retryOn5xx(true)
                .retryOn429(true)
                .build())
        .build();
```

`RetryPolicy.builder().enabled(false).build()` disables retries entirely (useful
in WireMock tests where you want to assert single-shot behaviour).

## Exception hierarchy

```
RuntimeException
└── KsefException                       // base — carries statusCode + responseBody
    ├── KsefAuthException               // HTTP 401 / 403 — re-auth or permission denied
    │   └── KsefSessionExpiredException // session-specific 401 — auto-handled by KsefClient
    ├── KsefNotFoundException           // HTTP 404 / 410
    ├── KsefRateLimitException          // HTTP 429 — RetryPolicy retries unless disabled
    ├── KsefServerException             // HTTP 5xx — RetryPolicy retries unless disabled
    ├── KsefNetworkException            // I/O errors / timeouts — statusCode == 0
    └── KsefCryptoException             // encryption / signing failures
```

Catch `KsefException` for any KSeF-side failure; catch one of the subclasses
for surgical handling (e.g. `KsefRateLimitException` to back off, or
`KsefNotFoundException` for soft-fail on a missing invoice).

## Logging

The SDK uses SLF4J. Default level should be `WARN` — see
[root README "Logging"](../README.md#logging) for the rationale and the
diagnostic-mode logback snippet.

Bodies are never logged: NIPs, PESELs, JWT tokens, AES keys, and full invoice
XML would all be RODO-classified personal data if leaked into logs.

## Batch invoice upload

> **Threading warning:** This method blocks the calling thread for minutes to
> hours, depending on batch size and upload bandwidth. KSeF batch can be up to
> 5 GB. Do not call from UI threads, HTTP request handlers, or reactive
> framework dispatch threads. Wrap with a dedicated executor for async use.

```java
List<Invoice> invoices = List.of(
        Invoice.fromXml(FormCode.FA3, Files.readAllBytes(Path.of("inv-1.xml"))),
        Invoice.fromXml(FormCode.FA3, Files.readAllBytes(Path.of("inv-2.xml"))));

BatchResult result = client.invoices().submitBatch(
        FormCode.FA3, invoices, BatchOptions.defaults());
```

`submitBatch` runs the full open / upload / close / poll / fetch-UPOs pipeline as a single synchronous call. For finer-grained progress reporting, wrap the call in your own executor — the SDK does not provide a callback / listener (per project ADR — inversion-of-control mismatched with SDK-as-API-tool framing).

## Architecture and design decisions

The full set of architectural decisions — generation strategy, package layout,
retry semantics, encryption flow, session abstractions — lives in
[`ADR/`](../ADR/) at the repository root. Sixteen ADRs as of this release,
each immutable in body once accepted (only `Status:` changes after the fact).

Implementation plan and roadmap are tracked in the root [CHANGELOG.md](../CHANGELOG.md)
and the [`ADR/`](../ADR/) set.

## License

[AGPL-3.0-only](../LICENSE.txt). See ADR-007 for the rationale (the original
plan to switch to Apache-2.0 at v1.0 was deprecated).
