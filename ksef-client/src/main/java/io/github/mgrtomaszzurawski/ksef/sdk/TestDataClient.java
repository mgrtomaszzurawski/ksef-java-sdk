/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk;

import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.AttachmentPermissionRevokeRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.BlockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonCreateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetRateLimitsRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetSessionLimitsRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SetSubjectLimitsRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectCreateRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.SubjectRemoveRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.TestDataPermissionsRevokeRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.UnblockContextAuthenticationRequestRaw;
import io.github.mgrtomaszzurawski.ksef.sdk.http.HttpSupport;

/**
 * Client for KSeF test environment data management — creating/removing test subjects,
 * persons, permissions, attachments, context blocking, and limit overrides.
 *
 * <p>All operations in this client are intended for the KSeF Test Environment only.
 * Some operations (subject, person, permissions, attachment, context block) do not
 * require authentication. Limit and rate-limit operations require authentication.</p>
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases") // Not a test class — manages KSeF test environment data
public final class TestDataClient {

    // --- Unauthenticated paths ---
    private static final String PATH_SUBJECT = "/api/v2/testdata/subject";
    private static final String PATH_SUBJECT_REMOVE = "/api/v2/testdata/subject/remove";
    private static final String PATH_PERSON = "/api/v2/testdata/person";
    private static final String PATH_PERSON_REMOVE = "/api/v2/testdata/person/remove";
    private static final String PATH_PERMISSIONS = "/api/v2/testdata/permissions";
    private static final String PATH_PERMISSIONS_REVOKE = "/api/v2/testdata/permissions/revoke";
    private static final String PATH_ATTACHMENT = "/api/v2/testdata/attachment";
    private static final String PATH_ATTACHMENT_REVOKE = "/api/v2/testdata/attachment/revoke";
    private static final String PATH_CONTEXT_BLOCK = "/api/v2/testdata/context/block";
    private static final String PATH_CONTEXT_UNBLOCK = "/api/v2/testdata/context/unblock";

    // --- Authenticated paths ---
    private static final String PATH_SESSION_LIMITS = "/api/v2/testdata/limits/context/session";
    private static final String PATH_SUBJECT_LIMITS = "/api/v2/testdata/limits/subject/certificate";
    private static final String PATH_RATE_LIMITS = "/api/v2/testdata/rate-limits";
    private static final String PATH_RATE_LIMITS_PRODUCTION = "/api/v2/testdata/rate-limits/production";

    // --- Operation names ---
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

    private final HttpSupport http;
    private final SessionContext sessionContext;

    public TestDataClient(KsefClient ksef) {
        this.http = new HttpSupport(ksef);
        this.sessionContext = ksef.sessionContext();
    }

    // --- Subject management (unauthenticated) ---

    /**
     * Create a test subject (taxpayer entity) in the test environment.
     *
     * @param request subject creation parameters (NIP, type, description)
     */
    public void createSubject(SubjectCreateRequestRaw request) {
        http.postJsonNoContent(PATH_SUBJECT, request, OP_CREATE_SUBJECT);
    }

    /**
     * Remove a test subject from the test environment.
     *
     * @param request subject removal parameters (NIP)
     */
    public void removeSubject(SubjectRemoveRequestRaw request) {
        http.postJsonNoContent(PATH_SUBJECT_REMOVE, request, OP_REMOVE_SUBJECT);
    }

    // --- Person management (unauthenticated) ---

    /**
     * Create a test person in the test environment.
     *
     * @param request person creation parameters (NIP, PESEL, description)
     */
    public void createPerson(PersonCreateRequestRaw request) {
        http.postJsonNoContent(PATH_PERSON, request, OP_CREATE_PERSON);
    }

    /**
     * Remove a test person from the test environment.
     *
     * @param request person removal parameters (NIP)
     */
    public void removePerson(PersonRemoveRequestRaw request) {
        http.postJsonNoContent(PATH_PERSON_REMOVE, request, OP_REMOVE_PERSON);
    }

    // --- Permission management (unauthenticated) ---

    /**
     * Grant test permissions in the test environment.
     *
     * @param request permission grant parameters
     */
    public void grantPermissions(TestDataPermissionsGrantRequestRaw request) {
        http.postJsonNoContent(PATH_PERMISSIONS, request, OP_GRANT_PERMISSIONS);
    }

    /**
     * Revoke test permissions in the test environment.
     *
     * @param request permission revocation parameters
     */
    public void revokePermissions(TestDataPermissionsRevokeRequestRaw request) {
        http.postJsonNoContent(PATH_PERMISSIONS_REVOKE, request, OP_REVOKE_PERMISSIONS);
    }

    // --- Attachment management (unauthenticated) ---

    /**
     * Grant attachment permissions in the test environment.
     *
     * @param request attachment permission grant parameters
     */
    public void grantAttachment(AttachmentPermissionGrantRequestRaw request) {
        http.postJsonNoContent(PATH_ATTACHMENT, request, OP_GRANT_ATTACHMENT);
    }

    /**
     * Revoke attachment permissions in the test environment.
     *
     * @param request attachment permission revocation parameters
     */
    public void revokeAttachment(AttachmentPermissionRevokeRequestRaw request) {
        http.postJsonNoContent(PATH_ATTACHMENT_REVOKE, request, OP_REVOKE_ATTACHMENT);
    }

    // --- Context blocking (unauthenticated) ---

    /**
     * Block authentication for a context in the test environment.
     *
     * @param request context blocking parameters
     */
    public void blockContext(BlockContextAuthenticationRequestRaw request) {
        http.postJsonNoContent(PATH_CONTEXT_BLOCK, request, OP_BLOCK_CONTEXT);
    }

    /**
     * Unblock authentication for a context in the test environment.
     *
     * @param request context unblocking parameters
     */
    public void unblockContext(UnblockContextAuthenticationRequestRaw request) {
        http.postJsonNoContent(PATH_CONTEXT_UNBLOCK, request, OP_UNBLOCK_CONTEXT);
    }

    // --- Session limits (authenticated) ---

    /**
     * Set session limits override in the test environment.
     *
     * @param request session limits (online and batch)
     */
    public void setSessionLimits(SetSessionLimitsRequestRaw request) {
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_SESSION_LIMITS, request, token, OP_SET_SESSION_LIMITS);
    }

    /**
     * Reset session limits to defaults in the test environment.
     */
    public void resetSessionLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SESSION_LIMITS, token, OP_RESET_SESSION_LIMITS);
    }

    // --- Subject limits (authenticated) ---

    /**
     * Set subject certificate limits override in the test environment.
     *
     * @param request subject limits
     */
    public void setSubjectLimits(SetSubjectLimitsRequestRaw request) {
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_SUBJECT_LIMITS, request, token, OP_SET_SUBJECT_LIMITS);
    }

    /**
     * Reset subject certificate limits to defaults in the test environment.
     */
    public void resetSubjectLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_SUBJECT_LIMITS, token, OP_RESET_SUBJECT_LIMITS);
    }

    // --- Rate limits (authenticated) ---

    /**
     * Set rate limit overrides in the test environment.
     *
     * @param request rate limit values
     */
    public void setRateLimits(SetRateLimitsRequestRaw request) {
        String token = sessionContext.token();
        http.postJsonAuthenticatedNoContent(PATH_RATE_LIMITS, request, token, OP_SET_RATE_LIMITS);
    }

    /**
     * Reset rate limits to defaults in the test environment.
     */
    public void resetRateLimits() {
        String token = sessionContext.token();
        http.deleteAuthenticated(PATH_RATE_LIMITS, token, OP_RESET_RATE_LIMITS);
    }

    /**
     * Set production rate limits in the test environment.
     */
    public void setProductionRateLimits() {
        String token = sessionContext.token();
        http.postNoBodyAuthenticated(PATH_RATE_LIMITS_PRODUCTION, token, OP_SET_PRODUCTION_RATE_LIMITS);
    }
}
