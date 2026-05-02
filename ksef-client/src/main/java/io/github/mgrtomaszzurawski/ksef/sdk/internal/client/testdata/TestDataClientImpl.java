/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.internal.client.testdata;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataClient;
import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionRevokeRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BlockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataAuthenticationContextIdentifierRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UnblockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.client.auth.SessionContext;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.internal.runtime.transport.HttpSupport;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Client for KSeF test environment data management — creating/removing test subjects,
 * persons, permissions, attachments, context blocking, and limit overrides.
 *
 * <p>All operations in this client are intended for the KSeF Test Environment only.
 * Some operations (subject, person, permissions, attachment, context block) do not
 * require authentication. Limit and rate-limit operations require authentication.</p>
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Not a test class — manages KSeF test environment data
public final class TestDataClientImpl implements TestDataClient {

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

    private static final String ERR_NULL_BUILDER = "builder is required";
    private static final String ERR_NULL_SUBJECT_NIP = "subjectNip is required";
    private static final String ERR_NULL_NIP = "nip is required";
    private static final String ERR_NULL_IDENTIFIER_TYPE = "identifierType is required";
    private static final String ERR_NULL_IDENTIFIER_VALUE = "identifierValue is required";
    private static final String ERR_NULL_EXPECTED_END_DATE = "expectedEndDate is required";

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public TestDataClientImpl(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    /**
     * Create a test subject (taxpayer entity) in the test environment.
     *
     * @param builder subject creation builder (NIP, type, description)
     */
    @Override
    public void createSubject(TestSubjectCreateBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        http.postJsonNoContent(PATH_SUBJECT, builder.build(), OP_CREATE_SUBJECT);
    }

    /**
     * Remove a test subject from the test environment.
     *
     * @param subjectNip NIP of the subject to remove
     */
    @Override
    public void removeSubject(String subjectNip) {
        Objects.requireNonNull(subjectNip, ERR_NULL_SUBJECT_NIP);
        SubjectRemoveRequestRaw request = new SubjectRemoveRequestRaw();
        request.setSubjectNip(subjectNip);
        http.postJsonNoContent(PATH_SUBJECT_REMOVE, request, OP_REMOVE_SUBJECT);
    }

    /**
     * Create a test person in the test environment.
     *
     * @param builder person creation builder (NIP, PESEL, isBailiff, description)
     */
    @Override
    public void createPerson(TestPersonCreateBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        http.postJsonNoContent(PATH_PERSON, builder.build(), OP_CREATE_PERSON);
    }

    /**
     * Remove a test person from the test environment.
     *
     * @param nip NIP of the person to remove
     */
    @Override
    public void removePerson(String nip) {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        PersonRemoveRequestRaw request = new PersonRemoveRequestRaw();
        request.setNip(nip);
        http.postJsonNoContent(PATH_PERSON_REMOVE, request, OP_REMOVE_PERSON);
    }

    /**
     * Grant test permissions in the test environment.
     *
     * @param builder permission grant builder
     */
    @Override
    public void grantPermissions(TestPermissionsGrantBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        http.postJsonNoContent(PATH_PERMISSIONS, builder.build(), OP_GRANT_PERMISSIONS);
    }

    /**
     * Revoke test permissions in the test environment.
     *
     * @param builder permission revocation builder
     */
    @Override
    public void revokePermissions(TestPermissionsRevokeBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        http.postJsonNoContent(PATH_PERMISSIONS_REVOKE, builder.build(), OP_REVOKE_PERMISSIONS);
    }

    /**
     * Grant attachment permissions in the test environment.
     *
     * @param nip NIP of the subject to grant attachment permissions to
     */
    @Override
    public void grantAttachment(String nip) {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        AttachmentPermissionGrantRequestRaw request = new AttachmentPermissionGrantRequestRaw();
        request.setNip(nip);
        http.postJsonNoContent(PATH_ATTACHMENT, request, OP_GRANT_ATTACHMENT);
    }

    /**
     * Revoke attachment permissions in the test environment.
     *
     * @param nip NIP of the subject to revoke attachment permissions from
     */
    @Override
    public void revokeAttachment(String nip) {
        Objects.requireNonNull(nip, ERR_NULL_NIP);
        AttachmentPermissionRevokeRequestRaw request = new AttachmentPermissionRevokeRequestRaw();
        request.setNip(nip);
        http.postJsonNoContent(PATH_ATTACHMENT_REVOKE, request, OP_REVOKE_ATTACHMENT);
    }

    /**
     * Revoke attachment permissions in the test environment with an expected end date.
     *
     * @param nip NIP of the subject to revoke attachment permissions from
     * @param expectedEndDate expected end date for the revocation
     */
    @Override
    public void revokeAttachment(String nip, LocalDate expectedEndDate) {
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
        Objects.requireNonNull(identifierType, ERR_NULL_IDENTIFIER_TYPE);
        Objects.requireNonNull(identifierValue, ERR_NULL_IDENTIFIER_VALUE);
        TestDataAuthenticationContextIdentifierRaw identifier = new TestDataAuthenticationContextIdentifierRaw();
        identifier.setType(identifierType.toRaw());
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
        Objects.requireNonNull(identifierType, ERR_NULL_IDENTIFIER_TYPE);
        Objects.requireNonNull(identifierValue, ERR_NULL_IDENTIFIER_VALUE);
        TestDataAuthenticationContextIdentifierRaw identifier = new TestDataAuthenticationContextIdentifierRaw();
        identifier.setType(identifierType.toRaw());
        identifier.setValue(identifierValue);
        UnblockContextAuthenticationRequestRaw request = new UnblockContextAuthenticationRequestRaw();
        request.setContextIdentifier(identifier);
        http.postJsonNoContent(PATH_CONTEXT_UNBLOCK, request, OP_UNBLOCK_CONTEXT);
    }

    /**
     * Set session limits override in the test environment.
     *
     * @param builder session limits builder (online and batch)
     */
    @Override
    public void setSessionLimits(TestSessionLimitsBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_SESSION_LIMITS, builder.build(), token, OP_SET_SESSION_LIMITS);
    }

    /**
     * Reset session limits to defaults in the test environment.
     */
    @Override
    public void resetSessionLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SESSION_LIMITS, token, OP_RESET_SESSION_LIMITS);
    }

    /**
     * Set subject certificate limits override in the test environment.
     *
     * @param builder subject limits builder
     */
    @Override
    public void setSubjectLimits(TestSubjectLimitsBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_SUBJECT_LIMITS, builder.build(), token, OP_SET_SUBJECT_LIMITS);
    }

    /**
     * Reset subject certificate limits to defaults in the test environment.
     */
    @Override
    public void resetSubjectLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SUBJECT_LIMITS, token, OP_RESET_SUBJECT_LIMITS);
    }

    /**
     * Set rate limit overrides in the test environment.
     *
     * @param builder rate limits builder
     */
    @Override
    public void setRateLimits(TestRateLimitsBuilder builder) {
        Objects.requireNonNull(builder, ERR_NULL_BUILDER);
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_RATE_LIMITS, builder.build(), token, OP_SET_RATE_LIMITS);
    }

    /**
     * Reset rate limits to defaults in the test environment.
     */
    @Override
    public void resetRateLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_RATE_LIMITS, token, OP_RESET_RATE_LIMITS);
    }

    /**
     * Set production rate limits in the test environment.
     */
    @Override
    public void setProductionRateLimits() {
        String token = sessionContext.token();
        http.postNoBodyAuthenticated(PATH_RATE_LIMITS_PRODUCTION, token, OP_SET_PRODUCTION_RATE_LIMITS);
    }
}
