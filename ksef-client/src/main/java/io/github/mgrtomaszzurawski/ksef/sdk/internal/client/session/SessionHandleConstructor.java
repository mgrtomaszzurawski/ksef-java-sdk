/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.session;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.InvoiceClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefBatchSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.KsefSession;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.PreparedInvoiceExport;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.model.PartUploadRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.batch.BatchPackageBuilder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import java.util.List;

/**
 * Internal construction bridge for {@link KsefSession}, {@link KsefBatchSession},
 * and {@link PreparedInvoiceExport}.
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
 * @apiNote Internal — never call from consumer code.
 */
public final class SessionHandleConstructor {

    private static final String ERR_REFLECTIVE_CONSTRUCTION_FAILED =
            "SDK internal error: reflective construction of session handle failed";

    private static final Constructor<KsefSession> ONLINE_SESSION_CTOR;
    private static final Constructor<KsefBatchSession> BATCH_SESSION_CTOR_3_ARG;
    private static final Constructor<KsefBatchSession> BATCH_SESSION_CTOR_5_ARG;
    private static final Constructor<KsefBatchSession> BATCH_SESSION_CTOR_6_ARG;
    private static final Constructor<PreparedInvoiceExport> PREPARED_EXPORT_CTOR;

    static {
        try {
            ONLINE_SESSION_CTOR = makeAccessible(
                    KsefSession.class.getDeclaredConstructor(
                            SessionClient.class, String.class, byte[].class, byte[].class));
            BATCH_SESSION_CTOR_3_ARG = makeAccessible(
                    KsefBatchSession.class.getDeclaredConstructor(
                            SessionClient.class, String.class, List.class));
            BATCH_SESSION_CTOR_5_ARG = makeAccessible(
                    KsefBatchSession.class.getDeclaredConstructor(
                            SessionClient.class, HttpClient.class, String.class,
                            List.class, BatchPackageBuilder.BatchPackage.class));
            BATCH_SESSION_CTOR_6_ARG = makeAccessible(
                    KsefBatchSession.class.getDeclaredConstructor(
                            SessionClient.class, HttpClient.class, String.class,
                            List.class, BatchPackageBuilder.BatchPackage.class,
                            java.util.function.LongSupplier.class));
            PREPARED_EXPORT_CTOR = makeAccessible(
                    PreparedInvoiceExport.class.getDeclaredConstructor(
                            InvoiceClient.class, HttpClient.class, String.class,
                            byte[].class, byte[].class));
        } catch (NoSuchMethodException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private SessionHandleConstructor() { }

    private static <T> Constructor<T> makeAccessible(Constructor<T> ctor) {
        ctor.setAccessible(true);
        return ctor;
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefSession newOnlineSession(SessionClient sessionClient,
                                                 String referenceNumber,
                                                 byte[] aesKey,
                                                 byte[] initVector) {
        return invoke(ONLINE_SESSION_CTOR, sessionClient, referenceNumber, aesKey, initVector);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests) {
        return invoke(BATCH_SESSION_CTOR_3_ARG, sessionClient, referenceNumber, partUploadRequests);
    }

    /**
     * @apiNote Internal — see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    HttpClient httpClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests,
                                                    BatchPackageBuilder.BatchPackage batchPackage) {
        return invoke(BATCH_SESSION_CTOR_5_ARG, sessionClient, httpClient,
                referenceNumber, partUploadRequests, batchPackage);
    }

    /**
     * @apiNote Internal — overload with injectable nano-time source for
     * upload-budget tests; see class-level Javadoc.
     */
    public static KsefBatchSession newBatchSession(SessionClient sessionClient,
                                                    HttpClient httpClient,
                                                    String referenceNumber,
                                                    List<PartUploadRequest> partUploadRequests,
                                                    BatchPackageBuilder.BatchPackage batchPackage,
                                                    java.util.function.LongSupplier nanoTimeSource) {
        return invoke(BATCH_SESSION_CTOR_6_ARG, sessionClient, httpClient,
                referenceNumber, partUploadRequests, batchPackage, nanoTimeSource);
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

    private static <T> T invoke(Constructor<T> ctor, Object... args) {
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
