# Third-Party Notices

This project bundles official KSeF API specification and XSD schema files
from the [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs)
repository and adapts demo assets and identifier-generation logic from
the [CIRFMF/ksef-client-java](https://github.com/CIRFMF/ksef-client-java)
reference SDK, both distributed under the MIT License. The bundled
spec files are inputs to the OpenAPI Generator and JAXB XJC build steps;
the generated Java classes derived from them are part of the SDK and
follow the project's AGPL-3.0-only license.

## Bundled files

The following files are copies of upstream artifacts from
[CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs):

- `ksef-client/openapi/open-api.json`
- `ksef-client/xsd/**`

## Adapted files

The following files in the demo module (`ksef-demo`, not published to
Maven Central) are derived from
[CIRFMF/ksef-client-java](https://github.com/CIRFMF/ksef-client-java):

- `ksef-demo/src/main/resources/invoice-templates/pef3.xml` and
  `pef_kor3.xml` — adapted from the official `demo-web-app` PEPPOL UBL
  invoice samples.
- `ksef-demo/src/main/java/io/github/mgrtomaszzurawski/ksef/sample/util/IdentifierGenerators.java` —
  Polish NIP / Peppol id / VAT-UE compound generation logic adapted
  from the upstream `IdentifierGeneratorUtils`.
- `ksef-demo/src/main/java/io/github/mgrtomaszzurawski/ksef/sample/util/SelfSignedCerts.java` —
  X.509 DN shape (organizationIdentifier OID 2.5.4.97 prefix
  conventions) inspired by the upstream
  `DefaultCertificateService.getCompanySeal`.

## Upstream license

```
MIT License

Copyright (c) 2025 Ministerstwo Finansów

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Runtime dependencies

The published `ksef-client` JAR has the following runtime dependencies
(non-exhaustive — see `mvn dependency:tree` for transitives). Each is
listed with its declared upstream license. Notable: EU DSS components
ship under **LGPL 2.1**; the SDK links them via the standard Java
classloader, satisfying LGPL relinking requirements automatically.

### Jackson — Apache License 2.0

- `com.fasterxml.jackson.core:jackson-core`
- `com.fasterxml.jackson.core:jackson-databind`
- `com.fasterxml.jackson.core:jackson-annotations`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310`

Upstream: [github.com/FasterXML/jackson](https://github.com/FasterXML/jackson)
License text: <https://www.apache.org/licenses/LICENSE-2.0>
NOTICE: each Jackson JAR ships its own `META-INF/NOTICE.txt`; preserved
when the SDK's shaded distribution is built.

### jackson-databind-nullable — Apache License 2.0

- `org.openapitools:jackson-databind-nullable`

Upstream: [github.com/OpenAPITools/jackson-databind-nullable](https://github.com/OpenAPITools/jackson-databind-nullable)
License text: <https://www.apache.org/licenses/LICENSE-2.0>

### Bouncy Castle — MIT-style (Bouncy Castle Licence)

- `org.bouncycastle:bcprov-jdk18on`
- `org.bouncycastle:bcpkix-jdk18on`

Upstream: [bouncycastle.org](https://www.bouncycastle.org/)
License text: <https://www.bouncycastle.org/licence.html>

### EU DSS — LGPL 2.1

- `eu.europa.ec.joinup.sd-dss:dss-xades`
- `eu.europa.ec.joinup.sd-dss:dss-token`
- `eu.europa.ec.joinup.sd-dss:dss-utils-apache-commons`

Upstream: [github.com/esig/dss](https://github.com/esig/dss)
License text: <https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>

The SDK links DSS dynamically (standard JVM classloading); recipients
may replace DSS JARs with a modified version satisfying the LGPL
relinking obligation. AGPL-3.0-only is compatible with LGPL-2.1
under the GPL/LGPL dependency-linking exception.

### ZXing — Apache License 2.0

- `com.google.zxing:core`
- `com.google.zxing:javase`

Upstream: [github.com/zxing/zxing](https://github.com/zxing/zxing)
License text: <https://www.apache.org/licenses/LICENSE-2.0>

### SLF4J — MIT License

- `org.slf4j:slf4j-api`

Upstream: [www.slf4j.org](https://www.slf4j.org/)
License text: <https://www.slf4j.org/license.html>

### JSpecify — Apache License 2.0

- `org.jspecify:jspecify`

Upstream: [jspecify.dev](https://jspecify.dev/)
License text: <https://www.apache.org/licenses/LICENSE-2.0>

### Jakarta XML Bind / Annotation / JAXB Runtime — EPL 2.0 + GPL 2.0 with Classpath Exception

- `jakarta.annotation:jakarta.annotation-api`
- `jakarta.xml.bind:jakarta.xml.bind-api`
- `org.glassfish.jaxb:jaxb-runtime`

Upstream: [eclipse-ee4j.github.io](https://eclipse-ee4j.github.io/)
License texts: <https://www.eclipse.org/legal/epl-2.0/>,
<https://openjdk.org/legal/gplv2+ce.html>

## Project license

The SDK source code (everything not listed above) is licensed under
**AGPL-3.0-only**. See [LICENSE.txt](LICENSE.txt).
