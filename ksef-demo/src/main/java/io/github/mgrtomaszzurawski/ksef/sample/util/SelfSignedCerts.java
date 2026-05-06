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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Generates short-lived self-signed X.509 certificates with the
 * specific X500 DN shape KSeF expects for company-seal authentication
 * (XAdES auth flow against {@code /v2/auth/xades-signature}).
 *
 * <p>Per upstream {@code CIRFMF/ksef-client-java} {@code DefaultCertificateService.getCompanySeal}
 * KSeF reads the authenticated identity from the
 * <strong>organizationIdentifier RDN (OID 2.5.4.97)</strong>, not the
 * Common Name. The DN must therefore include
 * {@code O=<org name>}, {@code 2.5.4.97=<identifier>}, {@code CN=<id>},
 * {@code C=PL}. A bare {@code CN=peppolId} is rejected with
 * {@code code=21115 "Nieprawidłowy certyfikat"}.
 *
 * <p>For NIP context KSeF expects the organizationIdentifier prefix
 * {@code VATPL-} followed by the NIP. For Peppol provider context the
 * organizationIdentifier matches the {@code peppolId} as-is. For
 * VAT-UE context the organizationIdentifier carries
 * {@code VATPL-{nip}} (just the NIP, not the full compound) — the
 * VAT-UE compound is sent as the {@code KsefIdentifier.nipVatUe(...)}
 * value separately, and KSeF resolves identity by the cert's SHA-256
 * fingerprint after a prior {@code EuEntityAdminPermission} grant
 * (see {@code VatUeProviderRunner}). Earlier
 * {@code VATPL-{nip}-{euCountryCode}{specific}} shape attempts in
 * direct-XAdES-auth were rejected by KSeF; do not reintroduce.
 *
 * <p>The certificate is throw-away — generated, used once for the
 * XAdES auth challenge signature, then discarded with the demo run.
 */
public final class SelfSignedCerts {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE_BITS = 2048;
    /** X.509 serial number bit length. 64 bits is plenty of entropy for throw-away demo certs. */
    private static final int SERIAL_BIT_LENGTH = 64;
    private static final long DEFAULT_VALIDITY_HOURS = 24L;
    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_ORGANIZATION_NAME = "KSeF Java SDK Demo";
    private static final String DEFAULT_COUNTRY_CODE = "PL";
    private static final String VATPL_PREFIX = "VATPL-";
    private static final String ERR_GENERATE = "Failed to generate self-signed certificate: ";

    private SelfSignedCerts() { }

    /**
     * Generate a self-signed company-seal certificate authenticating
     * the supplied NIP context. KSeF expects the organizationIdentifier
     * to carry the {@code VATPL-} prefix.
     */
    public static GeneratedCertificate forNip(String nip) {
        return generate(DEFAULT_ORGANIZATION_NAME, VATPL_PREFIX + nip, nip);
    }

    /**
     * Generate a self-signed company-seal certificate authenticating
     * the supplied Peppol provider id. The peppolId is placed in the
     * organizationIdentifier RDN as-is (no prefix).
     */
    public static GeneratedCertificate forPeppolId(String peppolId) {
        return generate(DEFAULT_ORGANIZATION_NAME, peppolId, peppolId);
    }

    /**
     * Generate a self-signed company-seal certificate authenticating
     * the supplied EU VAT-UE context. The organizationIdentifier RDN
     * carries {@code VATPL-{polishNip}} (just the NIP, KSeF derives the
     * EU VAT compound from the {@code <NipVatUe>} element in the
     * request body). The full compound {@code {nip}-{euCountryCode}...}
     * is the {@code KsefIdentifier.nipVatUe(...)} value passed
     * separately by the caller.
     *
     * @param vatUeCompound full compound id of shape {@code {nip}-{country}{specific}}
     */
    public static GeneratedCertificate forVatUe(String vatUeCompound) {
        int dashIndex = vatUeCompound.indexOf('-');
        String nip = dashIndex > 0 ? vatUeCompound.substring(0, dashIndex) : vatUeCompound;
        return generate(DEFAULT_ORGANIZATION_NAME, VATPL_PREFIX + nip, vatUeCompound);
    }

    /**
     * Generate a self-signed certificate with explicit RDN values.
     */
    public static GeneratedCertificate generate(String organizationName,
                                                  String organizationIdentifier,
                                                  String commonName) {
        return generate(organizationName, organizationIdentifier, commonName,
                Duration.ofHours(DEFAULT_VALIDITY_HOURS));
    }

    /**
     * Generate a self-signed certificate with explicit validity window.
     */
    public static GeneratedCertificate generate(String organizationName,
                                                  String organizationIdentifier,
                                                  String commonName,
                                                  Duration validity) {
        try {
            X500NameBuilder dnBuilder = new X500NameBuilder(BCStyle.INSTANCE);
            dnBuilder.addRDN(BCStyle.ORGANIZATION_IDENTIFIER, organizationIdentifier);
            dnBuilder.addRDN(BCStyle.O, organizationName);
            dnBuilder.addRDN(BCStyle.CN, commonName);
            dnBuilder.addRDN(BCStyle.C, DEFAULT_COUNTRY_CODE);
            X500Name x500Name = dnBuilder.build();

            KeyPair keyPair = generateKeyPair();
            Instant now = Instant.now();
            Date notBefore = Date.from(now.minusMillis(ONE_HOUR_MILLIS));
            Date notAfter = Date.from(now.plus(validity));
            BigInteger serial = new BigInteger(SERIAL_BIT_LENGTH, RANDOM);
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    x500Name, serial, notBefore, notAfter, x500Name, keyPair.getPublic());
            ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                    .build(keyPair.getPrivate());
            X509CertificateHolder holder = certBuilder.build(signer);
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);
            return new GeneratedCertificate(cert, keyPair.getPrivate());
        } catch (CertificateException | OperatorCreationException ex) {
            throw new IllegalStateException(ERR_GENERATE + ex.getMessage(), ex);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            gen.initialize(KEY_SIZE_BITS, RANDOM);
            return gen.generateKeyPair();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ERR_GENERATE + ex.getMessage(), ex);
        }
    }

    /** Self-signed X.509 certificate bundled with its private key. */
    public record GeneratedCertificate(X509Certificate certificate, PrivateKey privateKey) { }
}
