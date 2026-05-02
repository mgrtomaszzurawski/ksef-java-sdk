/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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

    public void createSubject(TestSubjectCreateBuilder builder);

    public void removeSubject(String subjectNip);

    public void createPerson(TestPersonCreateBuilder builder);

    public void removePerson(String nip);

    public void grantPermissions(TestPermissionsGrantBuilder builder);

    public void revokePermissions(TestPermissionsRevokeBuilder builder);

    public void grantAttachment(String nip);

    public void revokeAttachment(String nip);

    public void revokeAttachment(String nip, LocalDate expectedEndDate);

    public void blockContext(TestDataIdentifierType identifierType, String identifierValue);

    public void unblockContext(TestDataIdentifierType identifierType, String identifierValue);

    public void setSessionLimits(TestSessionLimitsBuilder builder);

    public void resetSessionLimits();

    public void setSubjectLimits(TestSubjectLimitsBuilder builder);

    public void resetSubjectLimits();

    public void setRateLimits(TestRateLimitsBuilder builder);

    public void resetRateLimits();

    public void setProductionRateLimits();

}
