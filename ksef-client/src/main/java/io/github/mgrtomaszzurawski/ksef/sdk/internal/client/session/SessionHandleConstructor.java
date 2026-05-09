/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import java.time.Duration;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import org.jspecify.annotations.Nullable;

/**
 * Internal construction bridge for the {@code OnlineSessionImpl}
 * (returned as {@link OnlineSession}) and
 * {@link PreparedInvoiceExport}.
 *
 * <p>Codex round-9 fresh-review F1: the previous {@code KsefSessionFactory}
 * lived in the exported {@code sdk.domain.invoicing} package, which meant
 * its public methods (taking internal-package types like {@code SessionClient}
 * and {@code BatchPackageBuilder.BatchPackage}) appeared in the published
 * binary/Javadoc surface. Moving the factory here — under
 * {@code sdk.internal.client.session}, which is NOT exported via
 * {@code module-info.java} — keeps the construction signatures invisible
 * to consumer modules and Javadoc.
 *
 * <p>The handle classes themselves stay in {@code sdk.domain.invoicing} (so
 * consumers can import them by name and use try-with-resources), but their
 * constructors are package-private. Cross-package access is achieved via
 * reflection ({@link Constructor#setAccessible(boolean)}); within the same
 * named module, this works without any {@code opens} directive in
 * {@code module-info.java} (JPMS only enforces module-boundary opens, not
 * package-boundary access within a module).
 *
 * <p>The reflective constructor lookups are performed once at class-load
 * time and cached as {@link Constructor} instances; subsequent
 * {@code newInstance} calls have negligible overhead vs. direct
 * instantiation.
 *
 * <p><strong>Future:</strong> if a future module-layout refactor splits
 * {@code sdk.domain.invoicing} into a separate JPMS module, the
 * reflective bridge becomes obsolete — replace with a package-level
 * factory class colocated with the handles, or use a JPMS {@code opens}
 * directive scoped to {@code sdk.internal.client.session}.
 *
 * @apiNote Internal — never call from consumer code.
 *
 * @since 1.0.0
 */
public final class SessionHandleConstructor {

    private static final String ERR_REFLECTIVE_CONSTRUCTION_FAILED =
            "SDK internal error: reflective construction of session handle failed";

    /**
     * Fully-qualified name of the package-private
     * {@code OnlineSessionImpl} (PR9 rename of {@code KsefSession}).
     * Resolved via {@link Class#forName(String)} because the impl class
     * is package-private inside {@code sdk.domain.invoicing} and not
     * importable from this internal package.
     */
    private static final String ONLINE_SESSION_IMPL_FQN =
            "io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.OnlineSessionImpl";

    private static final Constructor<? extends OnlineSession> ONLINE_SESSION_CTOR;
    private static final Constructor<? extends OnlineSession> ONLINE_SESSION_CTOR_VALID_UNTIL;
    private static final Constructor<? extends OnlineSession> ONLINE_SESSION_CTOR_VERIFICATION_AWARE;
    private static final Constructor<PreparedInvoiceExport> PREPARED_EXPORT_CTOR;

    static {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends OnlineSession> onlineImpl =
                    (Class<? extends OnlineSession>) Class.forName(ONLINE_SESSION_IMPL_FQN);
            ONLINE_SESSION_CTOR = makeAccessible(
                    onlineImpl.getDeclaredConstructor(
                            SessionClient.class, String.class, byte[].class, byte[].class));
            ONLINE_SESSION_CTOR_VALID_UNTIL = makeAccessible(
                    onlineImpl.getDeclaredConstructor(
                            SessionClient.class, String.class, byte[].class, byte[].class,
                            java.time.OffsetDateTime.class));
            ONLINE_SESSION_CTOR_VERIFICATION_AWARE = makeAccessible(
                    onlineImpl.getDeclaredConstructor(
                            SessionClient.class, String.class, byte[].class, byte[].class,
                            java.time.OffsetDateTime.class,
                            KsefEnvironment.class, Duration.class));
            PREPARED_EXPORT_CTOR = makeAccessible(
                    PreparedInvoiceExport.class.getDeclaredConstructor(
                            InvoiceClient.class, HttpClient.class, String.class,
                            byte[].class, byte[].class));
        } catch (NoSuchMethodException | ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private SessionHandleConstructor() { }

    /**
     * setAccessible is intentional and the whole point of this bridge —
     * see class-level Javadoc (Codex round-9 fresh-review F1). PMD's
     * AvoidAccessibilityAlteration rule is the right default for most
     * code, but here it would defeat the design.
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static <T> Constructor<T> makeAccessible(Constructor<T> ctor) {
        ctor.setAccessible(true);
        return ctor;
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static OnlineSession newOnlineSession(SessionClient sessionClient,
                                                 String referenceNumber,
                                                 byte[] aesKey,
                                                 byte[] initVector) {
        return invoke(ONLINE_SESSION_CTOR, sessionClient, referenceNumber, aesKey, initVector);
    }

    /**
     * @apiNote Internal — F8a variant carrying the open-response
     *     {@code validUntil} into the handle.
     */
    public static OnlineSession newOnlineSession(SessionClient sessionClient,
                                                 String referenceNumber,
                                                 byte[] aesKey,
                                                 byte[] initVector,
                                                 java.time.OffsetDateTime validUntil) {
        return invoke(ONLINE_SESSION_CTOR_VALID_UNTIL, sessionClient,
                referenceNumber, aesKey, initVector, validUntil);
    }

    /**
     * @apiNote Internal — PR10 variant carrying the {@link KsefEnvironment}
     *     and {@link Duration verification timeout} the impl needs to
     *     drive the synchronous {@code sendInvoice(Invoice)} pipeline
     *     (poll {@code invoiceStatus} until terminal, parse KsefNumber,
     *     render KOD I QR).
     */
    public static OnlineSession newOnlineSession(SessionClient sessionClient,
                                                 String referenceNumber,
                                                 byte[] aesKey,
                                                 byte[] initVector,
                                                 java.time.@Nullable OffsetDateTime validUntil,
                                                 KsefEnvironment environment,
                                                 Duration invoiceVerificationTimeout) {
        return invoke(ONLINE_SESSION_CTOR_VERIFICATION_AWARE, sessionClient,
                referenceNumber, aesKey, initVector, validUntil, environment, invoiceVerificationTimeout);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static PreparedInvoiceExport newPreparedExport(InvoiceClient invoices,
                                                            HttpClient httpClient,
                                                            String referenceNumber,
                                                            byte[] aesKey,
                                                            byte[] initVector) {
        return invoke(PREPARED_EXPORT_CTOR, invoices, httpClient, referenceNumber, aesKey, initVector);
    }

    private static <T> T invoke(Constructor<T> ctor, @Nullable Object... args) {
        try {
            return ctor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new IllegalStateException(ERR_REFLECTIVE_CONSTRUCTION_FAILED, ex);
        } catch (InvocationTargetException invocationFailure) {
            Throwable cause = invocationFailure.getCause();
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (cause instanceof Error errorFailure) {
                throw errorFailure;
            }
            throw new IllegalStateException(ERR_REFLECTIVE_CONSTRUCTION_FAILED, cause);
        }
    }
}
