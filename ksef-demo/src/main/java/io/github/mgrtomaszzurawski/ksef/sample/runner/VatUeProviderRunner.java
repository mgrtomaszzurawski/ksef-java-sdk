/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sample.util.IdentifierGenerators;
import io.github.mgrtomaszzurawski.ksef.sample.util.SelfSignedCerts;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefAsync;
import io.github.mgrtomaszzurawski.ksef.sdk.config.CertificateSubjectIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefCertificateCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefAsyncStatus;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * TEST-env-only runner exercising the EU-VAT-UE authentication context.
 *
 * <p>Flow (matches upstream {@code CIRFMF/ksef-client-java
 * EuEntityRepresentativePermissionIntegrationTest} lines 87-104):
 *
 * <ol>
 *   <li>Owner (primary NIP-context client from {@link DemoContext})
 *       grants EU entity administration permission with
 *       {@code context = NipVatUe(generatedCompound)} and
 *       {@code subject = SHA-256(euEntityCert)}.</li>
 *   <li>Wait for grant terminal status.</li>
 *   <li>EU entity authenticates with
 *       {@code KsefIdentifier.nipVatUe(generatedCompound)} +
 *       {@link CertificateSubjectIdentifier#fingerprint(String)}
 *       (NOT subject DN — KSeF resolves the entity by the SHA-256
 *       registered in step 1).</li>
 *   <li>AuthSessions succeeds because KSeF now recognises the fingerprint
 *       in the NipVatUe context.</li>
 * </ol>
 *
 * <p>Earlier rounds tried plain XAdES auth with various
 * {@code organizationIdentifier} RDN shapes ({@code VATPL-{nip}},
 * {@code VATPL-{nip}-{country}{specific}}, {@code {nip}-{country}{specific}})
 * and got {@code code=21117 "Nieprawidłowy identyfikator podmiotu dla
 * wskazanego typu kontekstu"} every time. The fix is the grant→auth
 * pre-registration flow above; cert RDN content is irrelevant when
 * auth uses {@code certificateFingerprint}.
 *
 * <p>FULL mode only — actual live grant + auth against KSeF TEST.
 * Self-signed cert is throw-away; no cert quota consumed.
 */
public final class VatUeProviderRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VatUeProviderRunner.class);
    private static final String NAME = "vatUeProvider";
    private static final String OP_GRANT = "grantEuEntityAdmin";
    private static final String OP_AUTH = "authAsVatUe";
    private static final String LABEL = "[FA(3)/NIP_VAT_UE]";
    private static final Duration GRANT_AWAIT_TIMEOUT = Duration.ofSeconds(30);
    private static final String SHA_256 = "SHA-256";
    private static final String EU_ENTITY_NAME = "KSeF Java SDK Demo EU Entity";
    /**
     * Single demo address used for both EU entity and subject details — same
     * fictitious physical location since the demo creates one self-signed
     * EU entity that is also its own admin contact.
     */
    private static final String DEMO_ADDRESS = "ul. Demo 1, 00-000 Demo City, EU";
    private static final String SUBJECT_FULL_NAME = "EU Entity Admin";
    private static final String GRANT_DESCRIPTION = "VatUeProviderRunner — pre-register EU entity for NipVatUe-context auth";
    private static final int GRANT_SUCCESS_STATUS_CODE = 200;
    /** Number of leading hex chars of the cert fingerprint to surface in result messages. */
    private static final int FINGERPRINT_PREVIEW_LENGTH = 8;

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();
        // KSeF requires the polishNip part of the VAT-UE compound to match
        // the caller's auth context NIP — using generateRandomVatUe() (random
        // NIP) yields code=410 "Podane identyfikatory są niezgodne lub
        // pozostają w niewłaściwej relacji". Bind to owner's actual NIP.
        String nipVatUe = IdentifierGenerators.generateVatUeFor(context.nipIdentifier());
        SelfSignedCerts.GeneratedCertificate euCert = SelfSignedCerts.forVatUe(nipVatUe);
        String fingerprintHex;
        try {
            fingerprintHex = sha256HexOf(euCert);
        } catch (Exception failure) {
            results.add(RunResult.fail(NAME, OP_GRANT + LABEL, 0, errorMessage(failure)));
            return results;
        }

        if (!grantAdminFromOwner(context, nipVatUe, fingerprintHex, results)) {
            return results;
        }
        runAuth(context, nipVatUe, euCert, fingerprintHex, results);
        return results;
    }

    private boolean grantAdminFromOwner(DemoContext context, String nipVatUe,
                                         String fingerprintHex, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EuEntityAdminPermissionGrantBuilder builder = EuEntityAdminPermissionGrantBuilder
                    .forFingerprint(fingerprintHex)
                    .contextNipVatUe(nipVatUe)
                    .description(GRANT_DESCRIPTION)
                    .euEntityName(EU_ENTITY_NAME)
                    .subjectEntityByFingerprint(SUBJECT_FULL_NAME, DEMO_ADDRESS)
                    .euEntityDetails(EU_ENTITY_NAME, DEMO_ADDRESS);
            String referenceNumber = context.client().permissions().grantEuEntityAdmin(builder.build()).referenceNumber();
            var permissions = context.client().permissions();
            var status = KsefAsync.awaitTerminal(
                    new KsefAsync.Config<>(
                            OP_GRANT,
                            () -> permissions.getOperationStatus(referenceNumber),
                            opStatus -> opStatus.status() != null
                                    && opStatus.status().code() >= KsefAsyncStatus.TERMINAL_STATUS_CODE_THRESHOLD,
                            opStatus -> opStatus.status() == null ? null : opStatus.status().code(),
                            GRANT_AWAIT_TIMEOUT,
                            null));
            int code = status.status() == null ? -1 : status.status().code();
            String description = status.status() == null ? "" : status.status().description();
            if (code == GRANT_SUCCESS_STATUS_CODE) {
                results.add(RunResult.ok(NAME, OP_GRANT + LABEL, elapsed(start),
                        "nipVatUe=" + nipVatUe + " fingerprint="
                                + fingerprintHex.substring(0, FINGERPRINT_PREVIEW_LENGTH) + "..."));
                return true;
            }
            results.add(RunResult.fail(NAME, OP_GRANT + LABEL, elapsed(start),
                    "grant terminal status code=" + code + " description=" + description));
            return false;
        } catch (Exception failure) {
            results.add(RunResult.fail(NAME, OP_GRANT + LABEL, elapsed(start),
                    errorMessage(failure)));
            return false;
        }
    }

    private void runAuth(DemoContext context, String nipVatUe,
                          SelfSignedCerts.GeneratedCertificate euCert, String fingerprintHex,
                          List<RunResult> results) {
        long start = System.currentTimeMillis();
        KsefCertificateCredentials creds = new KsefCertificateCredentials(
                euCert.certificate(), euCert.privateKey(),
                KsefIdentifier.nipVatUe(nipVatUe),
                CertificateSubjectIdentifier.fingerprint(fingerprintHex));
        try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.custom(context.environment()))
                .credentials(creds)
                .retryPolicy(RetryPolicy.builder().build())
                .build()) {
            // Drive lazy auth via any authenticated read; no-op ifPresent
            // consumes the Optional per Sonar S2201.
            client.authSessions().streamAuthSessions().findAny().ifPresent(authSession -> { });
            LOGGER.info("[{}] {} authenticated as nipVatUe={}", NAME, LABEL, nipVatUe);
            results.add(RunResult.ok(NAME, OP_AUTH + LABEL, elapsed(start),
                    "nipVatUe=" + nipVatUe));
        } catch (Exception failure) {
            results.add(RunResult.fail(NAME, OP_AUTH + LABEL, elapsed(start),
                    errorMessage(failure)));
        }
    }

    private static String sha256HexOf(SelfSignedCerts.GeneratedCertificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest digest = MessageDigest.getInstance(SHA_256);
        byte[] hash = digest.digest(cert.certificate().getEncoded());
        return HexFormat.of().withUpperCase().formatHex(hash);
    }
}
