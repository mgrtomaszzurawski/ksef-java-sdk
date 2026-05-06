# Third-Party Notices

This project bundles official KSeF API specification and XSD schema files
from the [CIRFMF/ksef-docs](https://github.com/CIRFMF/ksef-docs)
repository, distributed under the MIT License. The bundled files are
inputs to the OpenAPI Generator and JAXB XJC build steps; the generated
Java classes derived from them are part of the SDK and follow the
project's AGPL-3.0-only license.

## Bundled files

The following files are copies of upstream artifacts:

- `ksef-client/openapi/open-api.json`
- `ksef-client/xsd/**`

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

## Project license

The SDK source code (everything not listed above) is licensed under
**AGPL-3.0-only**. See [LICENSE.txt](LICENSE.txt).
