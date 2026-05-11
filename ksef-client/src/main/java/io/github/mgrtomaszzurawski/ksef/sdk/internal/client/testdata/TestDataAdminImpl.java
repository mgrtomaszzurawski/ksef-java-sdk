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
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataAdmin;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata.mapping.TestdataMappers;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpRuntime;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.time.LocalDate;
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
    private static final String OP_SET_PRODUCTION_RATE_LIMITS = "setProductionRateLimits";

    private static final String ERR_NULL_REQUEST = "request is required";
    private static final String ERR_NULL_SUBJECT_NIP = "subjectNip is required";
    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_IDENTIFIER_TYPE = "identifierType is required";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifierValue is required";
    private static final String ERR_NULL_EXPECTED_END_DATE = "expectedEndDate is required";

    private final HttpSupport http;

    public TestDataAdminImpl(HttpRuntime runtime) {
        this.http = new HttpSupport(runtime);
    }

    /**
     * Create a test subject (taxpayer entity) in the test environment.
     *
     * @param request subject creation request (NIP, type, description)
     */
    @Override
    public void createSubject(TestSubjectCreateRequest request) {
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
    public void removeSubject(String subjectNip) {
        LOGGER.debug(LOG_CALL, OP_REMOVE_SUBJECT);
        Objects.requireNonNull(subjectNip, ERR_NULL_SUBJECT_NIP);
        SubjectRemoveRequestRaw request = new SubjectRemoveRequestRaw();
        request.setSubjectNip(subjectNip);
        http.postJsonNoContent(PATH_SUBJECT_REMOVE, request, OP_REMOVE_SUBJECT);
    }

    /**
     * Create a test person in the test environment.
     *
     * @param request person creation request (NIP, PESEL, isBailiff, description)
     */
    @Override
    public void createPerson(TestPersonCreateRequest request) {
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
    public void removePerson(String nip) {
        LOGGER.debug(LOG_CALL, OP_REMOVE_PERSON);
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        PersonRemoveRequestRaw request = new PersonRemoveRequestRaw();
        request.setNip(nip);
        http.postJsonNoContent(PATH_PERSON_REMOVE, request, OP_REMOVE_PERSON);
    }

    /**
     * Grant test permissions in the test environment.
     *
     * @param request permission grant request
     */
    @Override
    public void grantPermissions(TestPermissionsGrantRequest request) {
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
    public void grantAttachment(String nip) {
        LOGGER.debug(LOG_CALL, OP_GRANT_ATTACHMENT);
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        AttachmentPermissionGrantRequestRaw request = new AttachmentPermissionGrantRequestRaw();
        request.setNip(nip);
        http.postJsonNoContent(PATH_ATTACHMENT, request, OP_GRANT_ATTACHMENT);
    }

    /**
     * Revoke attachment permissions in the test environment with an expected end date.
     *
     * @param nip NIP of the subject to revoke attachment permissions from
     * @param expectedEndDate expected end date for the revocation
     */
    @Override
    public void revokeAttachment(String nip, LocalDate expectedEndDate) {
        LOGGER.debug(LOG_CALL, OP_REVOKE_ATTACHMENT);
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        Objects.requireNonNull(expectedEndDate, ERR_NULL_EXPECTED_END_DATE);
        AttachmentPermissionRevokeRequestRaw request = new AttachmentPermissionRevokeRequestRaw();
        request.setNip(nip);
        request.setExpectedEndDate(expectedEndDate);
        http.postJsonNoContent(PATH_ATTACHMENT_REVOKE, request, OP_REVOKE_ATTACHMENT);
    }

    /**
     * Block authentication for a context in the test environment.
     *
     * @param identifierType type of context identifier
     * @param identifierValue value of context identifier (e.g., NIP)
     */
    @Override
    public void blockContext(TestDataIdentifierType identifierType, String identifierValue) {
        LOGGER.debug(LOG_CALL, OP_BLOCK_CONTEXT);
        Objects.requireNonNull(identifierType, ERR_NULL_IDENTIFIER_TYPE);
        Objects.requireNonNull(identifierValue, ERR_NULL_IDENTIFIER_VALUE);
        TestDataAuthenticationContextIdentifierRaw identifier = new TestDataAuthenticationContextIdentifierRaw();
        identifier.setType(TestdataMappers.toTestDataAuthenticationContextIdentifierTypeRaw(identifierType));
        identifier.setValue(identifierValue);
        BlockContextAuthenticationRequestRaw request = new BlockContextAuthenticationRequestRaw();
        request.setContextIdentifier(identifier);
        http.postJsonNoContent(PATH_CONTEXT_BLOCK, request, OP_BLOCK_CONTEXT);
    }

    /**
     * Unblock authentication for a context in the test environment.
     *
     * @param identifierType type of context identifier
     * @param identifierValue value of context identifier (e.g., NIP)
     */
    @Override
    public void unblockContext(TestDataIdentifierType identifierType, String identifierValue) {
        LOGGER.debug(LOG_CALL, OP_UNBLOCK_CONTEXT);
        Objects.requireNonNull(identifierType, ERR_NULL_IDENTIFIER_TYPE);
        Objects.requireNonNull(identifierValue, ERR_NULL_IDENTIFIER_VALUE);
        TestDataAuthenticationContextIdentifierRaw identifier = new TestDataAuthenticationContextIdentifierRaw();
        identifier.setType(TestdataMappers.toTestDataAuthenticationContextIdentifierTypeRaw(identifierType));
        identifier.setValue(identifierValue);
        UnblockContextAuthenticationRequestRaw request = new UnblockContextAuthenticationRequestRaw();
        request.setContextIdentifier(identifier);
        http.postJsonNoContent(PATH_CONTEXT_UNBLOCK, request, OP_UNBLOCK_CONTEXT);
    }

    /**
     * Set session limits override in the test environment.
     *
     * @param request session limits request (online and batch)
     */
    @Override
    public void setSessionLimits(TestSessionLimitsRequest request) {
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
        LOGGER.debug(LOG_CALL, OP_RESET_RATE_LIMITS);
        String token = http.requireToken();
        http.deleteAuthenticated(PATH_RATE_LIMITS, token, OP_RESET_RATE_LIMITS);
    }

    /**
     * Set production rate limits in the test environment.
     */
    @Override
    public void setProductionRateLimits() {
        LOGGER.debug(LOG_CALL, OP_SET_PRODUCTION_RATE_LIMITS);
        String token = http.requireToken();
        http.postNoBodyAuthenticated(PATH_RATE_LIMITS_PRODUCTION, token, OP_SET_PRODUCTION_RATE_LIMITS);
    }
}
