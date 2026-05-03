# ADR-019: KOD II signing scheme — RSA-PSS + ECDSA, auto-detected

Date: 2026-05-03
Status: Accepted

## Context

KSeF defines two QR code variants in `ksef-docs/kody-qr.md`:

- **KOD I** (online invoice verification) — anyone can build the URL given
  seller NIP, issue date, and the invoice file SHA-256. No cryptographic
  signing involved.
- **KOD II** (offline certificate verification) — the URL contains a
  cryptographic signature produced with the private key of a KSeF Offline
  certificate. The signature proves the invoice was issued by the holder of
  that certificate.

The spec at `kody-qr.md:197-210` mandates two signing algorithms for KOD II:

- **RSA** — RSASSA-PSS with SHA-256 digest, MGF1-SHA-256 mask, salt length
  32 bytes
- **ECDSA** — P-256 curve, SHA-256 digest, signature encoded either per
  IEEE P1363 (preferred) or DER

The signing payload is the QR URL host + path **without** the `https://`
prefix and **without** the trailing signature segment, UTF-8 encoded.

The SDK has so far been "PKI-neutral": `KsefVerificationLinks` exposes
`canonicalCertificateSigningPayload(...)` returning the bytes the consumer
must sign. The consumer signs with their own crypto stack
(JCA `Signature`, HSM, external signing service, etc.) and passes the
signature into `buildCertificateVerificationUrl(..., signature)`.

A round of cross-agent design discussion (`context/cross-agent-discussion.md`)
proposed adding a `KOD II` signing convenience that owns a `PrivateKey` and
performs the signing internally. That raised a concern about whether such a
convenience contradicts the PKI-neutral stance.

## Decision

The SDK will support **both** flows in 1.0.0:

1. **Pre-signed (canonical-payload) flow** — kept as-is. Consumers needing
   HSM, external signers, custom signing services, or non-Java PKI
   integrations call `canonicalCertificateSigningPayload(...)`, sign
   externally, and pass the resulting bytes to
   `buildCertificateVerificationUrl(..., signature)`.

2. **Convenience signing flow** — new `QrSigningService`
   (`sdk.domain.invoicing.qrcode.QrSigningService`) accepts a
   `KsefCertificateCredentials` and produces the fully signed verification
   URL. Internally it inspects the `PrivateKey` type and dispatches:
   - `RSAPrivateKey` → `Signature.getInstance("RSASSA-PSS")` with
     `PSSParameterSpec(SHA-256, MGF1, MGF1ParameterSpec.SHA256, saltLen=32, trailerField=1)`
   - `ECPrivateKey` → `Signature.getInstance("SHA256withECDSA")` for the
     P-256 curve, then re-encode the DER output as IEEE P1363 R||S.

The auto-detection from key type is **not** PKI lock-in. The spec defines
exactly two algorithms; auto-detection picks the one matching the supplied
key. Consumers needing an algorithm not in the spec are not in scope for
KSeF KOD II by definition.

The two flows live in **different services**:

- `KsefVerificationLinks` — pure URL construction (no signing). This is the
  primitive everyone can use.
- `QrSigningService` — convenience that consumes a
  `KsefCertificateCredentials` and emits a signed URL. This is opt-in.

`QrSigningService` is part of the public API. Its existence does not
deprecate the canonical-payload flow.

## Consequences

- 1.0.0 KOD II ergonomics: consumers with a Java `PrivateKey` get
  `client.qr().signingService().certificateVerificationUrl(...)` (one call).
  Consumers with HSM or external signers continue using the canonical-payload
  flow.
- The SDK does not promise support for any algorithm other than RSASSA-PSS
  with the spec parameters and ECDSA P-256/SHA-256. The Javadoc on
  `QrSigningService` cites this ADR.
- No public knob to "select algorithm". Algorithm follows the key type. If
  the consumer wants a different algorithm, they sign externally via the
  canonical-payload flow.
- ECDSA DER → P1363 conversion is implemented internally. We do not expose
  encoding format as a public option; `kody-qr.md:208-210` says P1363 is the
  preferred encoding and the spec accepts both, so we always emit P1363.

## Rejected alternatives

1. **Stay PKI-neutral only (no convenience).** Rejected because the spec
   defines exactly two algorithms; consumers with a normal Java
   `PrivateKey` should not have to learn `Signature.getInstance("RSASSA-PSS")`
   + `PSSParameterSpec` + DER-to-P1363 manually. That's friction without
   benefit.
2. **Expose algorithm enum on the convenience.** Rejected because the spec
   has only two algorithms and they map 1:1 to key types. An enum implies
   options that don't exist.
3. **Collapse `QrSigningService` and the XAdES `SigningService` into one
   `KsefSigningService`.** Rejected because XAdES (auth) and KOD II
   (QR) are different protocols with different signature formats, payload
   shapes, and trust contexts. Mixing them creates a leaky abstraction.

## Spec citations

- `ksef-docs/kody-qr.md:180-193` — KOD II URL structure
- `ksef-docs/kody-qr.md:197-210` — supported signing algorithms (RSA-PSS, ECDSA)
- `ksef-docs/kody-qr.md:212` — Base64URL signature encoding
- `ksef-docs/certyfikaty-KSeF.md` — KSeF Offline certificate type that owns
  the signing key

## Related

- ADR-005 — SDK overlay on generated code (records as public API)
- ADR-016 — `KsefClient` single entry point (`client.qr()` accessor)
- REQ-QR-14 through REQ-QR-18 in `context/SPEC-CONFORMANCE-AUDIT-2026-05-03-1600.md`
