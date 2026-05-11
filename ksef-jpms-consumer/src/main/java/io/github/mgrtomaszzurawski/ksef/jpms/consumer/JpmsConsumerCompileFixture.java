/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.jpms.consumer;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.KsefNumber;
import io.github.mgrtomaszzurawski.ksef.sdk.common.PublicKeyCertificate;
import io.github.mgrtomaszzurawski.ksef.sdk.common.StatusInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.config.FeaturePolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import io.github.mgrtomaszzurawski.ksef.sdk.config.RetryPolicy;
import io.github.mgrtomaszzurawski.ksef.sdk.config.SigningOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.config.UpoVersion;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.CsrResult;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.EncryptionMaterial;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.FileMetadata;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefCryptoService;
import io.github.mgrtomaszzurawski.ksef.sdk.crypto.KsefEncryptionInfo;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.authentication.model.AuthSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.FormCode;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.builder.InvoiceQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchOptions;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.BatchResult;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.FailedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.UpoEntry;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.KsefVerificationLinks;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrContextType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.qrcode.QrSigningService;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.DecryptedInvoice;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.sync.IncrementalSyncPlan;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Compile-only fixture (TC-ARCH-001) that names every public package the
 * SDK exports plus a representative type from each. Compiling this module
 * proves:
 *
 * <ul>
 *   <li>every type returned, accepted, or referenced by SDK public methods
 *       is resolvable from a JPMS consumer (no internal-package leak),</li>
 *   <li>the entry point {@link KsefClient} and its method shapes match the
 *       documented surface,</li>
 *   <li>the typed exception hierarchy is reachable.</li>
 * </ul>
 *
 * <p>This class never runs. The compiler check is the gate. If a future
 * SDK change moves a returned type into {@code sdk.internal.*} or
 * {@code client.model.*Raw}, this module fails to build — a regression
 * the reflection-based {@code PublicApiSurfaceTest} cannot catch on its
 * own (e.g. an earlier {@code AuthSession} JPMS-export gap that the
 * reflection-only check did not catch).
 */
public final class JpmsConsumerCompileFixture {

    private JpmsConsumerCompileFixture() {
        // not instantiable
    }

    /**
     * Reference every exported public package via at least one type, so the
     * compiler validates that each is in fact exported. A future regression
     * in {@code module-info.java} that drops an export breaks this build.
     */
    public static void referencePublicSurface() {
        // sdk + sdk.config + sdk.common
        Class<?>[] surface = {
                KsefClient.class,
                KsefEnvironment.class,
                KsefIdentifier.class,
                KsefTokenCredentials.class,
                RetryPolicy.class,
                FeaturePolicy.class,
                UpoVersion.class,
                SigningOptions.class,
                KsefNumber.class,
                StatusInfo.class,
                PublicKeyCertificate.class,
                // sdk.crypto
                KsefCryptoService.class,
                EncryptionMaterial.class,
                KsefEncryptionInfo.class,
                FileMetadata.class,
                CsrRequest.class,
                CsrResult.class,
                // sdk.domain.authentication.model — verify exported (gap caught earlier)
                AuthSession.class,
                // sdk.domain.invoicing + builder + qrcode + sync
                FormCode.class,
                OnlineSession.class,
                PreparedInvoiceExport.class,
                InvoiceQueryBuilder.class,
                // PR11 — batch submission API surface (model package)
                BatchOptions.class,
                BatchResult.class,
                FailedInvoice.class,
                UpoEntry.class,
                KsefVerificationLinks.class,
                QrEnvironment.class,
                QrContextType.class,
                QrSigningService.class,
                IncrementalSyncPlan.class,
                DecryptedInvoice.class,
                // sdk.exception
                KsefException.class,
        };
        if (surface.length == 0) {
            throw new IllegalStateException("Compile-only — class array referenced for compilation; never runs");
        }
    }

    /**
     * Exercise the public method shapes the way a real consumer would, so
     * the compiler validates parameter and return types reach exported
     * packages only.
     */
    @SuppressWarnings("unused")
    public static void referencePublicMethods() {
        KsefClient.Builder builder = KsefClient.builder().environment(KsefEnvironment.DEMO)
                .credentials(new KsefTokenCredentials("token", "1234567890"))
                .retryPolicy(RetryPolicy.builder().build())
                .features(FeaturePolicy.defaults());

        // Reference KsefClient public methods that return types from each domain.
        // No actual KsefClient is constructed (would require live network /
        // credentials) — the cast keeps the compiler honest about return types.
        Class<? extends KsefClient> clientClass = KsefClient.class;
        for (java.lang.reflect.Method method : clientClass.getMethods()) {
            Type returnType = method.getGenericReturnType();
            if (returnType.getTypeName().contains(".internal.")
                    || returnType.getTypeName().contains(".client.model.")) {
                throw new IllegalStateException(
                        "Public method leaks internal/raw type at compile time: " + method);
            }
        }

        // Build a sync plan referencing public types.
        IncrementalSyncPlan plan = IncrementalSyncPlan.builder()
                .from(OffsetDateTime.now())
                .outputDirectory(Path.of("/tmp/sync"))
                .build();
        if (plan == null) {
            throw new IllegalStateException("plan was null");
        }

        // Reference QR signing public-key payload helper without invoking it.
        KsefVerificationLinks.CertificateSigningInput input =
                new KsefVerificationLinks.CertificateSigningInput(
                        QrContextType.NIP, "1234567890", "1234567890",
                        "0123456789ABCDEF", new byte[32]);
        if (input == null) {
            throw new IllegalStateException("input was null");
        }

        // Reference Stream<AuthSession> as a public return shape (now via
        // the client.auth() accessor — PR6 trim).
        java.util.function.Function<KsefClient, java.util.stream.Stream<AuthSession>> streamSessions =
                client -> client.auth().streamAuthSessions();
        if (streamSessions == null) {
            throw new IllegalStateException("streamSessions was null");
        }

        // Builder + KsefIdentifier + cred types compile-checked
        if (builder == null) {
            throw new IllegalStateException("builder was null");
        }
    }
}
