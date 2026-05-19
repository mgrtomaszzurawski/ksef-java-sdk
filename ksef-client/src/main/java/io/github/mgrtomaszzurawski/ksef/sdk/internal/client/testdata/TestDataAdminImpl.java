/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata;

import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionRevokeRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BlockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthenticationContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UnblockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataAdmin;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefUnsupportedEnvironmentException;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestAttachmentRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping.TestdataMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for KSeF test environment data management — creating/removing test subjects,
 * persons, permissions, attachments, context blocking, and limit overrides.
 *
 * <p>All operations in this client are intended for the KSeF Test Environment only.
 * Some operations (subject, person, permissions, attachment, context block) do not
 * require authentication. Limit and rate-limit operations require authentication.</p>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Not a test class — manages KSeF test environment data
public final class TestDataAdminImpl implements TestDataAdmin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataAdminImpl.class);
    private static final String LOG_CALL = "→ {}";

    private static final String PATH_SUBJECT = ApiPaths.TESTDATA + "/subject";
    private static final String PATH_SUBJECT_REMOVE = ApiPaths.TESTDATA + "/subject/remove";
    private static final String PATH_PERSON = ApiPaths.TESTDATA + "/person";
    private static final String PATH_PERSON_REMOVE = ApiPaths.TESTDATA + "/person/remove";
    private static final String PATH_PERMISSIONS = ApiPaths.TESTDATA + "/permissions";
    private static final String PATH_PERMISSIONS_REVOKE = ApiPaths.TESTDATA + "/permissions/revoke";
    private static final String PATH_ATTACHMENT = ApiPaths.TESTDATA + "/attachment";
    private static final String PATH_ATTACHMENT_REVOKE = ApiPaths.TESTDATA + "/attachment/revoke";
    private static final String PATH_CONTEXT_BLOCK = ApiPaths.TESTDATA + "/context/block";
    private static final String PATH_CONTEXT_UNBLOCK = ApiPaths.TESTDATA + "/context/unblock";

    private static final String PATH_SESSION_LIMITS = ApiPaths.TESTDATA + "/limits/context/session";
    private static final String PATH_SUBJECT_LIMITS = ApiPaths.TESTDATA + "/limits/subject/certificate";
    private static final String PATH_RATE_LIMITS = ApiPaths.TESTDATA + "/rate-limits";
    private static final String PATH_RATE_LIMITS_PRODUCTION = ApiPaths.TESTDATA + "/rate-limits/production";

    private static final String OP_CREATE_SUBJECT = "createTestSubject";
    private static final String OP_REMOVE_SUBJECT = "removeTestSubject";
    private static final String OP_CREATE_PERSON = "createTestPerson";
    private static final String OP_REMOVE_PERSON = "removeTestPerson";
    private static final String OP_GRANT_PERMISSIONS = "grantTestPermissions";
    private static final String OP_REVOKE_PERMISSIONS = "revokeTestPermissions";
    private static final String OP_GRANT_ATTACHMENT = "grantTestAttachment";
    private static final String OP_REVOKE_ATTACHMENT = "revokeTestAttachment";
    private static final String OP_BLOCK_CONTEXT = "blockContext";
    private static final String OP_UNBLOCK_CONTEXT = "unblockContext";
    private static final String OP_SET_SESSION_LIMITS = "setSessionLimits";
    private static final String OP_RESET_SESSION_LIMITS = "resetSessionLimits";
    private static final String OP_SET_SUBJECT_LIMITS = "setSubjectLimits";
    private static final String OP_RESET_SUBJECT_LIMITS = "resetSubjectLimits";
    private static final String OP_SET_RATE_LIMITS = "setRateLimits";
    private static final String OP_RESET_RATE_LIMITS = "resetRateLimits";
    private static final String OP_SET_PRODUCTION_RATE_LIMITS = "applyProductionRateLimitsToTestTenant";

    private static final String ERR_NULL_REQUEST = "request is required";
    private static final String ERR_NULL_SUBJECT_NIP = "subjectIdentifier is required";
    private static final String ERR_NULL_NIP = "identifier is required";
    private static final String ERR_NON_NIP_IDENTIFIER =
            "TestDataAdmin operations require a NIP-typed KsefIdentifier but got: ";
    private static final String ERR_NULL_IDENTIFIER = "identifier must not be null";
    private static final String ERR_NULL_REVOKE_REQUEST = "request must not be null";
    private static final String ERR_NULL_ENVIRONMENT = "environment must not be null";
    private static final String ERR_PROD =
            "Test-data API is not available on KsefEnvironment.PROD — "
                    + "use TEST or DEMO for test-tenant operations.";

    private final HttpSupport http;
    private final KsefEnvironment environment;

    public TestDataAdminImpl(HttpRuntime runtime, KsefEnvironment environment) {
        this.http = new HttpSupport(runtime);
        this.environment = Objects.requireNonNull(environment, ERR_NULL_ENVIRONMENT);
    }

    /**
     * Fail-fast guard before any wire traffic. PROD has no test-data
     * endpoints; calling this surface there would resolve to HTTP 404
     * at the server. Throwing a typed exception locally gives consumers
     * a clear message and lets them branch on
     * {@link KsefUnsupportedEnvironmentException} instead of parsing
     * server error bodies.
     *
     * <p>Reference-equality guard: only catches {@link KsefEnvironment#PROD}
     * singleton. Custom URLs targeting the PROD host bypass on purpose —
     * the caller chose {@code custom(url)} deliberately and the SDK
     * does not pattern-match URLs.
     */
    private void ensureNotProd() {
        if (KsefEnvironment.PROD.equals(environment)) {
            throw new KsefUnsupportedEnvironmentException(ERR_PROD);
        }
    }

    /**
     * Create a test subject (taxpayer entity) in the test environment.
     *
     * @param request subject creation request (NIP, type, description)
     */
    @Override
    public void createSubject(TestSubjectCreateRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_CREATE_SUBJECT);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        http.postJsonNoContent(PATH_SUBJECT,
                TestdataMappers.toSubjectCreateRequestRaw(request), OP_CREATE_SUBJECT);
    }

    /**
     * Remove a test subject from the test environment.
     *
     * @param subjectNip NIP of the subject to remove
     */
    @Override
    public void removeSubject(KsefIdentifier subjectIdentifier) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_REMOVE_SUBJECT);
        String nipValue = requireNipValue(subjectIdentifier, ERR_NULL_SUBJECT_NIP);
        SubjectRemoveRequestRaw request = new SubjectRemoveRequestRaw();
        request.setSubjectNip(nipValue);
        http.postJsonNoContent(PATH_SUBJECT_REMOVE, request, OP_REMOVE_SUBJECT);
    }

    /**
     * Create a test person in the test environment.
     *
     * @param request person creation request (NIP, PESEL, isBailiff, description)
     */
    @Override
    public void createPerson(TestPersonCreateRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_CREATE_PERSON);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        http.postJsonNoContent(PATH_PERSON,
                TestdataMappers.toPersonCreateRequestRaw(request), OP_CREATE_PERSON);
    }

    /**
     * Remove a test person from the test environment.
     *
     * @param nip NIP of the person to remove
     */
    @Override
    public void removePerson(KsefIdentifier personIdentifier) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_REMOVE_PERSON);
        String nipValue = requireNipValue(personIdentifier, ERR_NULL_NIP);
        PersonRemoveRequestRaw request = new PersonRemoveRequestRaw();
        request.setNip(nipValue);
        http.postJsonNoContent(PATH_PERSON_REMOVE, request, OP_REMOVE_PERSON);
    }

    /**
     * Grant test permissions in the test environment.
     *
     * @param request permission grant request
     */
    @Override
    public void grantPermissions(TestPermissionsGrantRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_GRANT_PERMISSIONS);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        http.postJsonNoContent(PATH_PERMISSIONS,
                TestdataMappers.toTestDataPermissionsGrantRequestRaw(request), OP_GRANT_PERMISSIONS);
    }

    /**
     * Revoke test permissions in the test environment.
     *
     * @param request permission revocation request
     */
    @Override
    public void revokePermissions(TestPermissionsRevokeRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_REVOKE_PERMISSIONS);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        http.postJsonNoContent(PATH_PERMISSIONS_REVOKE,
                TestdataMappers.toTestDataPermissionsRevokeRequestRaw(request), OP_REVOKE_PERMISSIONS);
    }

    /**
     * Grant attachment permissions in the test environment.
     *
     * @param nip NIP of the subject to grant attachment permissions to
     */
    @Override
    public void grantAttachment(KsefIdentifier subjectIdentifier) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_GRANT_ATTACHMENT);
        String nipValue = requireNipValue(subjectIdentifier, ERR_NULL_NIP);
        AttachmentPermissionGrantRequestRaw request = new AttachmentPermissionGrantRequestRaw();
        request.setNip(nipValue);
        http.postJsonNoContent(PATH_ATTACHMENT, request, OP_GRANT_ATTACHMENT);
    }

    /**
     * Revoke attachment permission in the test environment. The wire's
     * {@code expectedEndDate} is nullable per OpenAPI; when the
     * request carries {@code null}, the SDK omits the field so the
     * server applies its default revocation behaviour.
     */
    @Override
    public void revokeAttachment(TestAttachmentRevokeRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_REVOKE_ATTACHMENT);
        Objects.requireNonNull(request, ERR_NULL_REVOKE_REQUEST);
        String nipValue = requireNipValue(request.subject(), ERR_NULL_NIP);
        AttachmentPermissionRevokeRequestRaw rawBody = new AttachmentPermissionRevokeRequestRaw();
        rawBody.setNip(nipValue);
        if (request.expectedEndDate() != null) {
            rawBody.setExpectedEndDate(request.expectedEndDate());
        }
        http.postJsonNoContent(PATH_ATTACHMENT_REVOKE, rawBody, OP_REVOKE_ATTACHMENT);
    }

    private static String requireNipValue(KsefIdentifier identifier, String nullMessage) {
        Objects.requireNonNull(identifier, nullMessage);
        if (identifier.type() != KsefIdentifier.Type.NIP) {
            throw new IllegalArgumentException(
                    ERR_NON_NIP_IDENTIFIER + identifier.type());
        }
        return identifier.value();
    }

    /**
     * Block authentication for a context in the test environment.
     *
     * @param identifier the context identifier (type + value)
     */
    @Override
    public void blockContext(KsefIdentifier identifier) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_BLOCK_CONTEXT);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        TestDataAuthenticationContextIdentifierRaw raw = new TestDataAuthenticationContextIdentifierRaw();
        raw.setType(TestdataMappers.toTestDataAuthenticationContextIdentifierTypeRaw(identifier.type()));
        raw.setValue(identifier.value());
        BlockContextAuthenticationRequestRaw request = new BlockContextAuthenticationRequestRaw();
        request.setContextIdentifier(raw);
        http.postJsonNoContent(PATH_CONTEXT_BLOCK, request, OP_BLOCK_CONTEXT);
    }

    /**
     * Unblock authentication for a context in the test environment.
     *
     * @param identifier the context identifier (type + value)
     */
    @Override
    public void unblockContext(KsefIdentifier identifier) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_UNBLOCK_CONTEXT);
        Objects.requireNonNull(identifier, ERR_NULL_IDENTIFIER);
        TestDataAuthenticationContextIdentifierRaw raw = new TestDataAuthenticationContextIdentifierRaw();
        raw.setType(TestdataMappers.toTestDataAuthenticationContextIdentifierTypeRaw(identifier.type()));
        raw.setValue(identifier.value());
        UnblockContextAuthenticationRequestRaw request = new UnblockContextAuthenticationRequestRaw();
        request.setContextIdentifier(raw);
        http.postJsonNoContent(PATH_CONTEXT_UNBLOCK, request, OP_UNBLOCK_CONTEXT);
    }

    /**
     * Set session limits override in the test environment.
     *
     * @param request session limits request (online and batch)
     */
    @Override
    public void setSessionLimits(TestSessionLimitsRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_SET_SESSION_LIMITS);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        String token = http.requireToken();
        http.postJsonAuthenticatedNoContent(PATH_SESSION_LIMITS,
                TestdataMappers.toSetSessionLimitsRequestRaw(request), token, OP_SET_SESSION_LIMITS);
    }

    /**
     * Reset session limits to defaults in the test environment.
     */
    @Override
    public void resetSessionLimits() {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_RESET_SESSION_LIMITS);
        String token = http.requireToken();
        http.deleteAuthenticated(PATH_SESSION_LIMITS, token, OP_RESET_SESSION_LIMITS);
    }

    /**
     * Set subject certificate limits override in the test environment.
     *
     * @param request subject limits request
     */
    @Override
    public void setSubjectLimits(TestSubjectLimitsRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_SET_SUBJECT_LIMITS);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        String token = http.requireToken();
        http.postJsonAuthenticatedNoContent(PATH_SUBJECT_LIMITS,
                TestdataMappers.toSetSubjectLimitsRequestRaw(request), token, OP_SET_SUBJECT_LIMITS);
    }

    /**
     * Reset subject certificate limits to defaults in the test environment.
     */
    @Override
    public void resetSubjectLimits() {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_RESET_SUBJECT_LIMITS);
        String token = http.requireToken();
        http.deleteAuthenticated(PATH_SUBJECT_LIMITS, token, OP_RESET_SUBJECT_LIMITS);
    }

    /**
     * Set rate limit overrides in the test environment.
     *
     * @param request rate limits request
     */
    @Override
    public void setRateLimits(TestRateLimitsRequest request) {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_SET_RATE_LIMITS);
        Objects.requireNonNull(request, ERR_NULL_REQUEST);
        String token = http.requireToken();
        http.postJsonAuthenticatedNoContent(PATH_RATE_LIMITS,
                TestdataMappers.toSetRateLimitsRequestRaw(request), token, OP_SET_RATE_LIMITS);
    }

    /**
     * Reset rate limits to defaults in the test environment.
     */
    @Override
    public void resetRateLimits() {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_RESET_RATE_LIMITS);
        String token = http.requireToken();
        http.deleteAuthenticated(PATH_RATE_LIMITS, token, OP_RESET_RATE_LIMITS);
    }

    /**
     * Set production rate limits in the test environment.
     */
    @Override
    public void applyProductionRateLimitsToTestTenant() {
        ensureNotProd();
        LOGGER.debug(LOG_CALL, OP_SET_PRODUCTION_RATE_LIMITS);
        String token = http.requireToken();
        http.postNoBodyAuthenticated(PATH_RATE_LIMITS_PRODUCTION, token, OP_SET_PRODUCTION_RATE_LIMITS);
    }
}
