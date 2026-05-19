/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata;

import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestAttachmentRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnsupportedEnvironmentException;

/**
 * KSeF test-environment data management — create/remove test subjects
 * and persons, grant/revoke test permissions, block/unblock contexts,
 * and override session / subject / rate limits for a test tenant.
 *
 * <p>Reached via {@code KsefClient.testData()}.
 *
 * <p><strong>Environment scope.</strong> Every operation here targets
 * KSeF endpoints that exist only on {@link KsefEnvironment#TEST} and
 * {@link KsefEnvironment#DEMO}. Calling any method on a client wired to
 * {@link KsefEnvironment#PROD} throws
 * {@link KsefUnsupportedEnvironmentException} before any wire traffic.
 * The check is reference-equality based; {@code KsefEnvironment.custom(url)}
 * targeting the PROD host bypasses the guard on purpose — the caller
 * chose {@code custom(url)} deliberately and the SDK does not pattern
 * match URLs.
 *
 * @since 1.0.0
 */
public interface TestDataAdmin {

    /**
     * Create a test subject (taxpayer entity) in the TEST/DEMO tenant.
     *
     * @param request subject creation request (NIP, type, description)
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void createSubject(TestSubjectCreateRequest request);

    /**
     * Remove a test subject from the TEST/DEMO tenant.
     *
     * @param subjectIdentifier NIP-typed identifier of the subject to remove
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void removeSubject(KsefIdentifier subjectIdentifier);

    /**
     * Create a test person in the TEST/DEMO tenant.
     *
     * @param request person creation request (NIP, PESEL, isBailiff, description)
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void createPerson(TestPersonCreateRequest request);

    /**
     * Remove a test person from the TEST/DEMO tenant.
     *
     * @param personIdentifier NIP-typed identifier of the person to remove
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void removePerson(KsefIdentifier personIdentifier);

    /**
     * Grant test permissions in the TEST/DEMO tenant.
     *
     * @param request permission grant request
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void grantPermissions(TestPermissionsGrantRequest request);

    /**
     * Revoke test permissions in the TEST/DEMO tenant.
     *
     * @param request permission revocation request
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void revokePermissions(TestPermissionsRevokeRequest request);

    /**
     * Grant attachment permissions to the given subject in the TEST/DEMO tenant.
     *
     * @param subjectIdentifier NIP-typed identifier of the subject
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void grantAttachment(KsefIdentifier subjectIdentifier);

    /**
     * Revoke a subject's attachment permission in the TEST/DEMO tenant.
     *
     * <p>Wire shape (per OpenAPI {@code AttachmentPermissionRevokeRequest}):
     * {@code nip} is required, {@code expectedEndDate} is nullable.
     * When the request's
     * {@link TestAttachmentRevokeRequest#expectedEndDate()} is
     * {@code null}, the field is omitted from the wire body and KSeF
     * applies its server-side default revocation behaviour per the
     * spec ("Data wycofania zgody na przesyłanie faktur z
     * załącznikiem").
     *
     * @param request the revocation request (non-null; carries
     *     {@code subject} and optional {@code expectedEndDate})
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void revokeAttachment(TestAttachmentRevokeRequest request);

    /**
     * Block authentication for a KSeF auth-context (identified by NIP /
     * INTERNAL_ID / NIP_VAT_UE / PEPPOL_ID per
     * {@link KsefIdentifier.Type}). After the call any auth attempt
     * targeting that context returns the KSeF "context blocked" error.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void blockContext(KsefIdentifier identifier);

    /**
     * Reverse a previous {@link #blockContext(KsefIdentifier)} for the
     * given context identifier.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void unblockContext(KsefIdentifier identifier);

    /**
     * Override session limits (online + batch) for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void setSessionLimits(TestSessionLimitsRequest request);

    /**
     * Reset session limits to defaults for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void resetSessionLimits();

    /**
     * Override subject certificate limits for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void setSubjectLimits(TestSubjectLimitsRequest request);

    /**
     * Reset subject certificate limits to defaults for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void resetSubjectLimits();

    /**
     * Override rate limits for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void setRateLimits(TestRateLimitsRequest request);

    /**
     * Reset rate limits to defaults for the current test tenant.
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void resetRateLimits();

    /**
     * Apply the production rate-limit profile to the current test tenant
     * (so the test environment behaves like PROD under load).
     *
     * @throws KsefUnsupportedEnvironmentException when the client is wired to {@link KsefEnvironment#PROD}
     */
    void applyProductionRateLimitsToTestTenant();

}
