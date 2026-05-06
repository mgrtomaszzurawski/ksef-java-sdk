/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import java.util.ArrayList;
import java.util.List;

/**
 * TEST-env-only runner exercising the EU-VAT-UE authentication context.
 *
 * <p>Per {@code KsefIdentifier.Type.NIP_VAT_UE} the SDK maps this to
 * the wire-level {@code AuthenticationContextIdentifierType=NipVatUe}
 * so the resulting session would be anchored to the EU VAT context.
 *
 * <p><strong>Currently always SKIPs.</strong> Live verification of
 * NipVatUe-context auth via on-the-fly self-signed cert is blocked on
 * KSeF returning {@code 21117 "Nieprawidłowy identyfikator podmiotu dla
 * wskazanego typu kontekstu"} for every cert organizationId shape we
 * have tried ({@code VATPL-{nip}}, {@code VATPL-{nip}-{country}{specific}},
 * {@code {nip}-{country}{specific}} alone). Upstream
 * {@code CIRFMF/ksef-client-java} has no equivalent integration test
 * for VAT-UE-context auth — only EU-entity permission grants, where
 * VAT-UE is the grant <em>target</em>, not the authenticated context.
 * The exact cert ↔ identifier coupling KSeF expects for this context
 * type is not derivable from the bundled XSDs alone. Wire-shape
 * coverage of NipVatUe credentials is provided by WireMock contract
 * tests in {@code KsefCredentialsTest}.
 *
 * <p>FULL mode only.
 */
public final class VatUeProviderRunner implements DemoRunner {

    private static final String NAME = "vatUeProvider";
    private static final String OP_AUTH = "authAsVatUe";
    private static final String LABEL = "[FA(3)/NIP_VAT_UE]";
    private static final String SKIP_MESSAGE =
            "VAT-UE auth context cert shape vs KSeF requirement is not known — "
                    + "upstream SDK has no live VAT-UE auth example. SDK supports the "
                    + "credential type (KsefIdentifier.nipVatUe + KsefCertificateCredentials), "
                    + "verified via WireMock contract tests.";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        results.add(RunResult.skip(NAME, OP_AUTH + LABEL, SKIP_MESSAGE));
        return results;
    }
}
