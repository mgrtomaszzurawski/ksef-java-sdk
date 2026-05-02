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

import io.github.mgrtomaszzurawski.ksef.sdk.domain.certificates.model.CertificateEnrollmentData;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/**
 * Generates a PKCS#10 CSR (DER) using subject fields supplied by KSeF
 * `getEnrollmentData`. Used by both `CertProbe` (standalone diagnostic) and
 * `CertificateRunner` (demo-app cert test gate).
 */
public final class CertificateCsrUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String DN_CN_PREFIX = "CN=";
    private static final String DN_COUNTRY_PREFIX = ",C=";
    private static final String DN_GIVEN_NAME_PREFIX = ",GIVENNAME=";
    private static final String DN_SURNAME_PREFIX = ",SURNAME=";
    private static final String DN_SERIAL_NUMBER_PREFIX = ",SERIALNUMBER=";
    private static final String DN_ORGANIZATION_PREFIX = ",O=";
    private static final String OID_ORGANIZATION_IDENTIFIER = "2.5.4.97";
    private static final String DN_ORGANIZATION_IDENTIFIER_PREFIX = "," + OID_ORGANIZATION_IDENTIFIER + "=";

    private CertificateCsrUtil() { }

    /**
     * Generate a fresh RSA key pair plus a CSR DER built from the supplied enrollment data.
     */
    public static CsrResult generate(CertificateEnrollmentData data)
            throws NoSuchAlgorithmException, OperatorCreationException, IOException {
        KeyPair keyPair = generateKeyPair();
        byte[] csrDer = buildCsr(data, keyPair);
        return new CsrResult(csrDer, keyPair);
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        return keyPairGenerator.generateKeyPair();
    }

    private static byte[] buildCsr(CertificateEnrollmentData data, KeyPair keyPair)
            throws OperatorCreationException, IOException {
        StringBuilder subjectDn = new StringBuilder();
        subjectDn.append(DN_CN_PREFIX).append(data.commonName());
        subjectDn.append(DN_COUNTRY_PREFIX).append(data.countryName());
        if (data.givenName() != null) {
            subjectDn.append(DN_GIVEN_NAME_PREFIX).append(data.givenName());
        }
        if (data.surname() != null) {
            subjectDn.append(DN_SURNAME_PREFIX).append(data.surname());
        }
        if (data.serialNumber() != null) {
            subjectDn.append(DN_SERIAL_NUMBER_PREFIX).append(data.serialNumber());
        }
        if (data.organizationName() != null) {
            subjectDn.append(DN_ORGANIZATION_PREFIX).append(data.organizationName());
        }
        if (data.organizationIdentifier() != null) {
            subjectDn.append(DN_ORGANIZATION_IDENTIFIER_PREFIX).append(data.organizationIdentifier());
        }
        X500Name subject = new X500Name(subjectDn.toString());
        JcaPKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
        PKCS10CertificationRequest certificationRequest = csrBuilder.build(signer);
        return certificationRequest.getEncoded();
    }

    /**
     * Tuple of CSR DER bytes and the key pair backing it. Caller keeps the key
     * pair if it intends to use the issued certificate; the demo runners discard
     * it because they revoke the cert immediately after the round-trip.
     *
     * <p>The compact constructor and accessor defensively clone the byte array so
     * the record exposes an independent copy of the CSR payload, consistent with
     * the SpotBugs {@code EI_EXPOSE_REP} mitigation applied elsewhere in the SDK.
     */
    public record CsrResult(byte[] csrDer, KeyPair keyPair) {
        public CsrResult {
            csrDer = csrDer.clone();
        }

        @Override
        public byte[] csrDer() {
            return csrDer.clone();
        }
    }
}
