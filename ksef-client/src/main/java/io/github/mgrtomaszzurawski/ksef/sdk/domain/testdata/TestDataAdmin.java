/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsGrantRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPermissionsRevokeRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestPersonCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestRateLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSessionLimitsRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectCreateRequest;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectLimitsRequest;
import java.time.LocalDate;

/**
 * Public API for TestDataAdmin.
 *
 * @since 1.0.0
 */
public interface TestDataAdmin {

    void createSubject(TestSubjectCreateRequest request);

    void removeSubject(String subjectNip);

    void createPerson(TestPersonCreateRequest request);

    void removePerson(String nip);

    void grantPermissions(TestPermissionsGrantRequest request);

    void revokePermissions(TestPermissionsRevokeRequest request);

    void grantAttachment(String nip);

    void revokeAttachment(String nip, LocalDate expectedEndDate);

    void blockContext(TestDataIdentifierType identifierType, String identifierValue);

    void unblockContext(TestDataIdentifierType identifierType, String identifierValue);

    void setSessionLimits(TestSessionLimitsRequest request);

    void resetSessionLimits();

    void setSubjectLimits(TestSubjectLimitsRequest request);

    void resetSubjectLimits();

    void setRateLimits(TestRateLimitsRequest request);

    void resetRateLimits();

    void applyProductionRateLimitsToTestTenant();

}
