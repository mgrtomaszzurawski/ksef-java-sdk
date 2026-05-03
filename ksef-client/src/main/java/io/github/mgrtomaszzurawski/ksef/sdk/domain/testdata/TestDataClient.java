/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata;

import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestDataIdentifierType;
import java.time.LocalDate;

/** Public API for TestDataClient. */
public interface TestDataClient {

    void createSubject(TestSubjectCreateBuilder builder);

    void removeSubject(String subjectNip);

    void createPerson(TestPersonCreateBuilder builder);

    void removePerson(String nip);

    void grantPermissions(TestPermissionsGrantBuilder builder);

    void revokePermissions(TestPermissionsRevokeBuilder builder);

    void grantAttachment(String nip);

    void revokeAttachment(String nip);

    void revokeAttachment(String nip, LocalDate expectedEndDate);

    void blockContext(TestDataIdentifierType identifierType, String identifierValue);

    void unblockContext(TestDataIdentifierType identifierType, String identifierValue);

    void setSessionLimits(TestSessionLimitsBuilder builder);

    void resetSessionLimits();

    void setSubjectLimits(TestSubjectLimitsBuilder builder);

    void resetSubjectLimits();

    void setRateLimits(TestRateLimitsBuilder builder);

    void resetRateLimits();

    void setProductionRateLimits();

}
