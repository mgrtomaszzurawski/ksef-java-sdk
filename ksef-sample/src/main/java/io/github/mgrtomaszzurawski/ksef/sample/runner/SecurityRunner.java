/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
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

import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PublicKeyCertificateUsageRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner for SecurityClient operations. Fetches KSeF public key certificates
 * and makes the encryption key available for subsequent runners.
 */
public final class SecurityRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityRunner.class);
    private static final String NAME = "security";
    private static final String OP_GET_CERTS = "getPublicKeyCertificates";
    private static final PublicKeyCertificateUsageRaw USAGE_TOKEN_ENCRYPTION =
            PublicKeyCertificateUsageRaw.KSEF_TOKEN_ENCRYPTION;
    private static final String CERT_TYPE = "X.509";
    private static final String ERR_NO_ENCRYPTION_CERT = "No certificate with usage KsefTokenEncryption found";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        long start = System.currentTimeMillis();
        try {
            List<PublicKeyCertificateRaw> certs = context.client().security().getPublicKeyCertificates();
            LOG.info("[{}] fetched {} certificates", NAME, certs.size());

            PublicKey encryptionKey = null;
            for (PublicKeyCertificateRaw cert : certs) {
                LOG.info("[{}] cert usage={}, valid={} to {}", NAME,
                        cert.getUsage(), cert.getValidFrom(), cert.getValidTo());
                if (cert.getUsage() != null && cert.getUsage().contains(USAGE_TOKEN_ENCRYPTION)) {
                    encryptionKey = extractPublicKey(cert.getCertificate());
                }
            }

            if (encryptionKey == null) {
                results.add(RunResult.fail(NAME, OP_GET_CERTS, elapsed(start), ERR_NO_ENCRYPTION_CERT));
            } else {
                context.setKsefPublicKey(encryptionKey);
                LOG.info("[{}] KsefTokenEncryption key extracted: {}", NAME, encryptionKey.getAlgorithm());
                results.add(RunResult.ok(NAME, OP_GET_CERTS, elapsed(start),
                        certs.size() + " certificates, encryption key: " + encryptionKey.getAlgorithm()));
            }
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_CERTS, elapsed(start), errorMessage(exception)));
        }
        return results;
    }

    private static PublicKey extractPublicKey(byte[] derCertBytes) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE);
        X509Certificate x509 = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(derCertBytes));
        return x509.getPublicKey();
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    private static String errorMessage(Exception exception) {
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}
