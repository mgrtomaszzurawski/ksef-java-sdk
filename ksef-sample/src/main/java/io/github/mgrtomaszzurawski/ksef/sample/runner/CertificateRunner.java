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

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationChallengeResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AuthenticationInitResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentDataResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.CertificateEnrollmentStatusResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.EnrollCertificateResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.KsefCertificateTypeRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.QueryCertificatesRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.RevokeCertificateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;

/**
 * Runner for CertificateClient operations. Tests getLimits with token auth,
 * then switches to XAdES auth session for operations that require it
 * (enrollmentData, query). Re-authenticates with token afterward.
 */
public final class CertificateRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateRunner.class);
    private static final String NAME = "certificate";
    private static final String OP_GET_LIMITS = "getLimits";
    private static final String OP_GET_ENROLLMENT_DATA = "getEnrollmentData";
    private static final String OP_QUERY = "query";
    private static final String OP_ENROLL = "enroll";
    private static final String OP_ENROLLMENT_STATUS = "getEnrollmentStatus";
    private static final String OP_REVOKE = "revoke";
    private static final String SKIP_NO_CERT = "no certificate available";
    private static final String CERT_NAME = "SDK Demo Certificate";
    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int AUTH_STATUS_OK = 200;
    private static final int POLL_INITIAL_DELAY_MS = 500;
    private static final int POLL_MAX_DELAY_MS = 5000;
    private static final int POLL_TIMEOUT_MS = 30000;
    private static final int POLL_BACKOFF_MULTIPLIER = 2;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Get limits (works with token auth)
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getLimits();
            LOG.info("[{}] certificate limits: canRequest={}", NAME, response.getCanRequest());
            results.add(RunResult.ok(NAME, OP_GET_LIMITS, elapsed(start),
                    "canRequest=" + response.getCanRequest()));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_LIMITS, elapsed(start), errorMessage(exception)));
        }

        // 2-3. Enrollment data + query — require XAdES auth session
        if (context.hasCertificate()) {
            runWithXadesSession(context, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_ENROLLMENT_DATA, SKIP_NO_CERT));
            results.add(RunResult.skip(NAME, OP_QUERY, SKIP_NO_CERT));
        }

        // 4. Enroll — handled inside XAdES session block above
        if (!context.hasCertificate()) {
            results.add(RunResult.skip(NAME, OP_ENROLL, SKIP_NO_CERT));
        }

        return results;
    }

    /**
     * Switch to XAdES session, run cert ops, then re-auth with token.
     */
    private void runWithXadesSession(DemoContext context, List<RunResult> results) {
        try {
            // Authenticate with XAdES
            AuthenticationChallengeResponseRaw challenge = context.client().auth().requestChallenge();
            AuthenticationInitResponseRaw authResp = context.client().auth()
                    .authenticateWithXades(challenge.getChallenge(),
                            context.certificate(), context.privateKey(), context.nipIdentifier());
            pollUntilReady(context, authResp.getReferenceNumber());
            context.client().auth().redeemTokens();
            LOG.info("[{}] switched to XAdES session for cert ops", NAME);

            // Run cert ops under XAdES session
            CertificateEnrollmentDataResponseRaw enrollmentData = runGetEnrollmentData(context, results);
            runQuery(context, results);
            runEnrollAndRevoke(context, enrollmentData, results);

            // Terminate XAdES session
            context.client().auth().terminateCurrentSession();
            LOG.info("[{}] XAdES session terminated", NAME);

            // Re-authenticate with token
            challenge = context.client().auth().requestChallenge();
            context.client().auth().authenticateWithToken(
                    challenge, context.ksefToken(), context.nipIdentifier(), context.ksefPublicKey());
            pollUntilReady(context, context.client().sessionContext().referenceNumber());
            context.client().auth().redeemTokens();
            LOG.info("[{}] re-authenticated with token", NAME);

        } catch (Exception exception) {
            LOG.error("[{}] XAdES session switch failed: {}", NAME, errorMessage(exception));
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, 0, errorMessage(exception)));
            results.add(RunResult.skip(NAME, OP_QUERY, "XAdES session failed"));
        }
    }

    private CertificateEnrollmentDataResponseRaw runGetEnrollmentData(
            DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().getEnrollmentData();
            LOG.info("[{}] enrollment data: cn={}, c={}, gn={}, sn={}, serial={}, o={}, oid={}",
                    NAME, response.getCommonName(), response.getCountryName(),
                    response.getGivenName(), response.getSurname(), response.getSerialNumber(),
                    response.getOrganizationName(), response.getOrganizationIdentifier());
            results.add(RunResult.ok(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start),
                    "cn=" + response.getCommonName()));
            return response;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ENROLLMENT_DATA, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private void runEnrollAndRevoke(DemoContext context,
                                    CertificateEnrollmentDataResponseRaw enrollmentData,
                                    List<RunResult> results) {
        if (enrollmentData == null) {
            results.add(RunResult.skip(NAME, OP_ENROLL, "no enrollment data"));
            return;
        }

        String enrollmentRef = null;
        long start = System.currentTimeMillis();
        try {
            // Generate RSA key pair + CSR
            byte[] csrBytes = generateCsr(enrollmentData);

            // Enroll
            EnrollCertificateRequestRaw request = new EnrollCertificateRequestRaw()
                    .certificateName(CERT_NAME)
                    .certificateType(KsefCertificateTypeRaw.AUTHENTICATION)
                    .csr(csrBytes);
            EnrollCertificateResponseRaw response = context.client().certificates().enroll(request);
            enrollmentRef = response.getReferenceNumber();
            LOG.info("[{}] enrolled certificate, ref={}", NAME, enrollmentRef);
            results.add(RunResult.ok(NAME, OP_ENROLL, elapsed(start), "ref=" + enrollmentRef));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_ENROLL, elapsed(start), errorMessage(exception)));
            return;
        }

        // Check enrollment status
        start = System.currentTimeMillis();
        String serialNumber = null;
        try {
            CertificateEnrollmentStatusResponseRaw status =
                    context.client().certificates().getEnrollmentStatus(enrollmentRef);
            LOG.info("[{}] enrollment status: code={}", NAME,
                    status.getStatus() != null ? status.getStatus().getCode() : "null");
            serialNumber = status.getCertificateSerialNumber();
            results.add(RunResult.ok(NAME, OP_ENROLLMENT_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_ENROLLMENT_STATUS, elapsed(start), errorMessage(exception)));
        }

        // Revoke (cleanup) — only if we got a serial number
        if (serialNumber != null) {
            start = System.currentTimeMillis();
            try {
                context.client().certificates().revoke(serialNumber, new RevokeCertificateRequestRaw());
                LOG.info("[{}] revoked certificate serial={}", NAME, serialNumber);
                results.add(RunResult.ok(NAME, OP_REVOKE, elapsed(start),
                        "revoked serial=" + serialNumber));
            } catch (Exception exception) {
                results.add(RunResult.fail(NAME, OP_REVOKE, elapsed(start), errorMessage(exception)));
            }
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE, "no serial number from enrollment"));
        }
    }

    private static byte[] generateCsr(CertificateEnrollmentDataResponseRaw data) throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGen.initialize(RSA_KEY_SIZE);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        StringBuilder subjectDn = new StringBuilder();
        subjectDn.append("CN=").append(data.getCommonName());
        subjectDn.append(",C=").append(data.getCountryName());
        if (data.getGivenName() != null) {
            subjectDn.append(",GIVENNAME=").append(data.getGivenName());
        }
        if (data.getSurname() != null) {
            subjectDn.append(",SURNAME=").append(data.getSurname());
        }
        if (data.getSerialNumber() != null) {
            subjectDn.append(",SERIALNUMBER=").append(data.getSerialNumber());
        }
        if (data.getOrganizationName() != null) {
            subjectDn.append(",O=").append(data.getOrganizationName());
        }
        if (data.getOrganizationIdentifier() != null) {
            subjectDn.append(",2.5.4.97=").append(data.getOrganizationIdentifier());
        }
        LOG.info("[{}] CSR subject DN: {}", NAME, subjectDn);

        X500Name subject = new X500Name(subjectDn.toString());
        JcaPKCS10CertificationRequestBuilder csrBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = csrBuilder.build(signer);
        return csr.getEncoded();
    }

    private void runQuery(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().certificates().query(new QueryCertificatesRequestRaw());
            int count = response.getCertificates() != null ? response.getCertificates().size() : 0;
            LOG.info("[{}] queried certificates: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY, elapsed(start), count + " certificates"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY, elapsed(start), errorMessage(exception)));
        }
    }

    private void pollUntilReady(DemoContext context, String referenceNumber) throws InterruptedException {
        int delay = POLL_INITIAL_DELAY_MS;
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            var response = context.client().auth().getStatus(referenceNumber);
            Integer code = response.getStatus() != null ? response.getStatus().getCode() : null;
            if (code != null && code == AUTH_STATUS_OK) {
                return;
            }
            Thread.sleep(delay);
            delay = Math.min(delay * POLL_BACKOFF_MULTIPLIER, POLL_MAX_DELAY_MS);
        }
        throw new IllegalStateException("Timeout waiting for auth status 200 for " + referenceNumber);
    }
}
