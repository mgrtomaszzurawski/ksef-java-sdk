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
package io.github.mgrtomaszzurawski.ksef.sample.util;

import java.security.SecureRandom;

/**
 * Random identifier generators for the KSeF TEST environment, where
 * KSeF auto-creates contexts on first authentication. Adapted from the
 * official {@code CIRFMF/ksef-client-java}
 * {@code IdentifierGeneratorUtils}.
 *
 * <p>Generated identifiers are not valid for DEMO/PROD — those
 * environments require pre-registered real identities. The generators
 * here are intended exclusively for {@code api-test.ksef.mf.gov.pl}.
 */
public final class IdentifierGenerators {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NIP_LENGTH = 10;
    private static final int[] NIP_CHECKSUM_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};
    private static final int CHECKSUM_MODULUS = 11;
    private static final int CHECKSUM_INVALID = 10;
    private static final int PEPPOL_DIGIT_COUNT = 6;
    private static final String PEPPOL_PREFIX = "PPL";
    private static final int VAT_UE_DIGIT_COUNT = 9;
    private static final String VAT_UE_PREFIX = "DE";

    private IdentifierGenerators() { }

    /**
     * Generate a random valid Polish NIP (10 digits, checksum-correct).
     * KSeF TEST env accepts any valid-shape NIP and auto-creates the
     * subject context on first auth.
     */
    public static String generateRandomNip() {
        while (true) {
            int[] digits = new int[NIP_LENGTH];
            digits[0] = RANDOM.nextInt(1, 10);
            for (int i = 1; i < NIP_LENGTH - 1; i++) {
                digits[i] = RANDOM.nextInt(0, 10);
            }
            int weightedSum = 0;
            for (int i = 0; i < NIP_CHECKSUM_WEIGHTS.length; i++) {
                weightedSum += digits[i] * NIP_CHECKSUM_WEIGHTS[i];
            }
            int checkDigit = weightedSum % CHECKSUM_MODULUS;
            if (checkDigit == CHECKSUM_INVALID) {
                continue;
            }
            digits[NIP_LENGTH - 1] = checkDigit;
            StringBuilder builder = new StringBuilder(NIP_LENGTH);
            for (int digit : digits) {
                builder.append(digit);
            }
            return builder.toString();
        }
    }

    /**
     * Generate a random Peppol participant id in the format
     * {@code PPL + 6 digits}. Per the upstream PeppolInvoiceIntegrationTest
     * pattern, KSeF TEST env auto-registers the provider on first XAdES
     * auth submitted with this peppolId as the certificate subject.
     */
    public static String generatePeppolId() {
        StringBuilder builder = new StringBuilder(PEPPOL_PREFIX.length() + PEPPOL_DIGIT_COUNT);
        builder.append(PEPPOL_PREFIX);
        for (int i = 0; i < PEPPOL_DIGIT_COUNT; i++) {
            builder.append(RANDOM.nextInt(0, 10));
        }
        return builder.toString();
    }

    /**
     * Generate a random KSeF VAT-UE compound identifier in the format
     * {@code {polishNip}-{euCountryCode}{country-specific}} required by
     * KSeF's {@code TNipVatUE} schema (per
     * {@code ksef-client/xsd/auth/*.xsd}, EU VIES validation pattern).
     *
     * <p>Example output: {@code 1111111111-DE123456789}.
     *
     * <p>{@code DE} is the EU country prefix used here (9 digits per
     * VIES); KSeF TEST env auto-creates the resulting VAT-UE context.
     */
    public static String generateRandomVatUe() {
        return generateVatUeFor(generateRandomNip());
    }

    /**
     * Generate a VAT-UE compound bound to the supplied Polish NIP. Use
     * this variant when the compound must reference an existing/owner
     * NIP context — e.g. when granting EU entity admin permissions
     * where KSeF requires {@code TNipVatUE.{nip}} to match the
     * caller's authentication NIP context (otherwise grant fails with
     * code=410 "Podane identyfikatory są niezgodne lub pozostają w
     * niewłaściwej relacji").
     *
     * @param polishNip the existing 10-digit Polish NIP to bind to
     * @return {@code {polishNip}-DE{9 random digits}}
     */
    public static String generateVatUeFor(String polishNip) {
        StringBuilder builder = new StringBuilder();
        builder.append(polishNip);
        builder.append('-');
        builder.append(VAT_UE_PREFIX);
        for (int i = 0; i < VAT_UE_DIGIT_COUNT; i++) {
            builder.append(RANDOM.nextInt(0, 10));
        }
        return builder.toString();
    }
}
