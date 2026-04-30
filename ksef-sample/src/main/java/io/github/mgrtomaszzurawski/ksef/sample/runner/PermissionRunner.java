/*
 * KSeF Sample App - Demo application exercising the KSeF Java SDK against the live demo server
 * Copyright © 2026 Tomasz Zurawski (${email})
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

import io.github.mgrtomaszzurawski.ksef.sdk.model.PermissionOperationResult;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for PermissionClient operations. Exercises every grant variant (person, entity,
 * authorization, indirect, subunit, EU entity admin, EU entity), every query variant
 * (personal, persons, subunits, entities, entity-roles, subordinate-roles, authorizations,
 * EU entities), both revoke variants, and the operation/attachment status endpoints.
 * <p>
 * Each grant is wrapped grant -> status -> revoke for self-cleanup. Failures are reported
 * per-operation; one failing call never short-circuits the rest.
 */
public final class PermissionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionRunner.class);
    private static final String NAME = "permission";

    // --- Operation labels ---
    private static final String OP_GRANT_PERSON = "grantPerson";
    private static final String OP_GRANT_ENTITY = "grantEntity";
    private static final String OP_GRANT_AUTHORIZATION = "grantAuthorization";
    private static final String OP_GRANT_INDIRECT = "grantIndirect";
    private static final String OP_GRANT_SUBUNIT = "grantSubunit";
    private static final String OP_GRANT_EU_ENTITY_ADMIN = "grantEuEntityAdmin";
    private static final String OP_GRANT_EU_ENTITY = "grantEuEntity";
    private static final String OP_GET_OP_STATUS = "getOperationStatus";
    private static final String OP_QUERY_PERSONAL = "queryPersonal";
    private static final String OP_QUERY_PERSONS = "queryPersons";
    private static final String OP_QUERY_SUBUNITS = "querySubunits";
    private static final String OP_QUERY_ENTITIES = "queryEntities";
    private static final String OP_QUERY_ENTITY_ROLES = "queryEntityRoles";
    private static final String OP_QUERY_SUBORDINATE_ROLES = "querySubordinateRoles";
    private static final String OP_QUERY_AUTHORIZATIONS = "queryAuthorizations";
    private static final String OP_QUERY_EU_ENTITIES = "queryEuEntities";
    private static final String OP_GET_ATTACHMENT = "getAttachmentStatus";
    private static final String OP_REVOKE_COMMON = "revokeCommon";
    private static final String OP_REVOKE_AUTHORIZATION = "revokeAuthorization";

    // --- Stable test data (reused across grants) ---
    private static final String TEST_PERSON_PESEL = "82060411457";
    private static final String TEST_PERSON_FIRST_NAME = "Jan";
    private static final String TEST_PERSON_LAST_NAME = "Kowalski";
    private static final String TEST_ENTITY_NIP = "1111111111";
    private static final String TEST_ENTITY_FULL_NAME = "Firma Testowa Sp. z o.o.";
    private static final String TEST_AUTHORIZATION_NIP = "2222222222";
    private static final String TEST_AUTHORIZATION_NAME = "Partner Sp. z o.o.";
    private static final String TEST_INDIRECT_NIP = "3333333333";
    private static final String TEST_SUBUNIT_CONTEXT_NIP = "4444444444";
    private static final String TEST_FINGERPRINT = "AABBCCDDEEFF00112233445566778899AABBCCDDEEFF00112233445566778899";
    private static final String TEST_EU_NIP_VAT_UE = "DE123456789";
    private static final String TEST_EU_ENTITY_NAME = "EU Partner GmbH";
    private static final String TEST_EU_ENTITY_ADDRESS = "Berlin, Germany";

    // --- Description strings ---
    private static final String GRANT_PERSON_DESC = "SDK Demo person grant - will be revoked";
    private static final String GRANT_ENTITY_DESC = "SDK Demo entity grant - will be revoked";
    private static final String GRANT_AUTH_DESC = "SDK Demo authorization grant - will be revoked";
    private static final String GRANT_INDIRECT_DESC = "SDK Demo indirect grant - will be revoked";
    private static final String GRANT_SUBUNIT_DESC = "SDK Demo subunit grant - will be revoked";
    private static final String GRANT_EU_ADMIN_DESC = "SDK Demo EU admin grant - will be revoked";
    private static final String GRANT_EU_ENTITY_DESC = "SDK Demo EU entity grant - will be revoked";

    private static final String DEPENDS_ON_GRANT = "depends on grant";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // --- 1. Grant + status + revoke cycles for each grant variant ---
        runPersonGrantCycle(context, results);
        runEntityGrantCycle(context, results);
        runAuthorizationGrantCycle(context, results);
        runIndirectGrantCycle(context, results);
        runSubunitGrantCycle(context, results);
        runEuEntityAdminGrantCycle(context, results);
        runEuEntityGrantCycle(context, results);

        // --- 2. Query variants (read-only, safe) ---
        runQueryPersonal(context, results);
        runQueryPersons(context, results);
        runQuerySubunits(context, results);
        runQueryEntities(context, results);
        runQueryEntityRoles(context, results);
        runQuerySubordinateRoles(context, results);
        runQueryAuthorizations(context, results);
        runQueryEuEntities(context, results);

        // --- 3. Attachment status ---
        runGetAttachmentStatus(context, results);

        return results;
    }

    // ==================== Grant cycles ====================

    private void runPersonGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantPerson(context, results);
        if (operationRef != null) {
            runGetOperationStatus(context, operationRef, results);
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_OP_STATUS + "[person]", DEPENDS_ON_GRANT));
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[person]", DEPENDS_ON_GRANT));
        }
    }

    private void runEntityGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantEntity(context, results);
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[entity]", DEPENDS_ON_GRANT));
        }
    }

    private void runAuthorizationGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantAuthorization(context, results);
        if (operationRef != null) {
            runRevokeAuthorization(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_AUTHORIZATION, DEPENDS_ON_GRANT));
        }
    }

    private void runIndirectGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantIndirect(context, results);
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[indirect]", DEPENDS_ON_GRANT));
        }
    }

    private void runSubunitGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantSubunit(context, results);
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[subunit]", DEPENDS_ON_GRANT));
        }
    }

    private void runEuEntityAdminGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantEuEntityAdmin(context, results);
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[euAdmin]", DEPENDS_ON_GRANT));
        }
    }

    private void runEuEntityGrantCycle(DemoContext context, List<RunResult> results) {
        String operationRef = runGrantEuEntity(context, results);
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON + "[euEntity]", DEPENDS_ON_GRANT));
        }
    }

    // ==================== Individual grant methods ====================

    private String runGrantPerson(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionGrantBuilder builder = PersonPermissionGrantBuilder
                    .forPesel(TEST_PERSON_PESEL)
                    .description(GRANT_PERSON_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME)
                    .invoiceRead();
            PermissionOperationResult response = context.client().permissions().grantPerson(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted person permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_PERSON, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_PERSON, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantEntity(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityPermissionGrantBuilder builder = EntityPermissionGrantBuilder
                    .forNip(TEST_ENTITY_NIP)
                    .description(GRANT_ENTITY_DESC)
                    .entityDetails(TEST_ENTITY_FULL_NAME)
                    .invoiceRead();
            PermissionOperationResult response = context.client().permissions().grantEntity(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted entity permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_ENTITY, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_ENTITY, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantAuthorization(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityAuthorizationPermissionGrantBuilder builder = EntityAuthorizationPermissionGrantBuilder
                    .forNip(TEST_AUTHORIZATION_NIP)
                    .description(GRANT_AUTH_DESC)
                    .entityDetails(TEST_AUTHORIZATION_NAME)
                    .selfInvoicing();
            PermissionOperationResult response = context.client().permissions().grantAuthorization(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted authorization permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_AUTHORIZATION, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_AUTHORIZATION, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantIndirect(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            IndirectPermissionGrantBuilder builder = IndirectPermissionGrantBuilder
                    .forNip(TEST_INDIRECT_NIP)
                    .description(GRANT_INDIRECT_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME)
                    .invoiceRead();
            PermissionOperationResult response = context.client().permissions().grantIndirect(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted indirect permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_INDIRECT, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_INDIRECT, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantSubunit(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SubunitPermissionGrantBuilder builder = SubunitPermissionGrantBuilder
                    .forPesel(TEST_PERSON_PESEL)
                    .contextNip(TEST_SUBUNIT_CONTEXT_NIP)
                    .description(GRANT_SUBUNIT_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME);
            PermissionOperationResult response = context.client().permissions().grantSubunit(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted subunit permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_SUBUNIT, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_SUBUNIT, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantEuEntityAdmin(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EuEntityAdminPermissionGrantBuilder builder = EuEntityAdminPermissionGrantBuilder
                    .forFingerprint(TEST_FINGERPRINT)
                    .contextNipVatUe(TEST_EU_NIP_VAT_UE)
                    .description(GRANT_EU_ADMIN_DESC)
                    .euEntityName(TEST_EU_ENTITY_NAME)
                    .subjectEntityByFingerprint(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS)
                    .euEntityDetails(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS);
            PermissionOperationResult response = context.client().permissions().grantEuEntityAdmin(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted EU entity admin permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_EU_ENTITY_ADMIN, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_EU_ENTITY_ADMIN, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    private String runGrantEuEntity(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EuEntityPermissionGrantBuilder builder = EuEntityPermissionGrantBuilder
                    .forFingerprint(TEST_FINGERPRINT)
                    .description(GRANT_EU_ENTITY_DESC)
                    .subjectEntityByFingerprint(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS)
                    .invoiceRead();
            PermissionOperationResult response = context.client().permissions().grantEuEntity(builder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted EU entity permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_EU_ENTITY, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_EU_ENTITY, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

    // ==================== Status / shared ====================

    private void runGetOperationStatus(DemoContext context, String referenceNumber, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().getOperationStatus(referenceNumber);
            LOG.info("[{}] operation status: code={}", NAME,
                    response.status() != null ? response.status().code() : "null");
            results.add(RunResult.ok(NAME, OP_GET_OP_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_OP_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGetAttachmentStatus(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().getAttachmentStatus();
            LOG.info("[{}] attachment allowed: {}", NAME, response.attachmentAllowed());
            results.add(RunResult.ok(NAME, OP_GET_ATTACHMENT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ATTACHMENT, elapsed(start), errorMessage(exception)));
        }
    }

    // ==================== Revoke methods ====================

    private void runRevokeCommon(DemoContext context, String permissionId, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().permissions().revokeCommon(permissionId);
            LOG.info("[{}] revoked permission id={}", NAME, permissionId);
            results.add(RunResult.ok(NAME, OP_REVOKE_COMMON, elapsed(start), "revoked " + permissionId));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE_COMMON, elapsed(start), errorMessage(exception)));
        }
    }

    private void runRevokeAuthorization(DemoContext context, String permissionId, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            context.client().permissions().revokeAuthorization(permissionId);
            LOG.info("[{}] revoked authorization id={}", NAME, permissionId);
            results.add(RunResult.ok(NAME, OP_REVOKE_AUTHORIZATION, elapsed(start), "revoked " + permissionId));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_REVOKE_AUTHORIZATION, elapsed(start), errorMessage(exception)));
        }
    }

    // ==================== Query methods ====================

    private void runQueryPersonal(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryPersonal(PersonalPermissionsQueryBuilder.create());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOG.info("[{}] personal permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_PERSONAL, elapsed(start), count + " permissions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PERSONAL, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryPersons(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionsQueryBuilder builder = PersonPermissionsQueryBuilder
                    .permissionsGrantedInCurrentContext();
            var response = context.client().permissions().queryPersons(builder);
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOG.info("[{}] person permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_PERSONS, elapsed(start), count + " permissions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PERSONS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQuerySubunits(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().querySubunits();
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOG.info("[{}] subunit permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_SUBUNITS, elapsed(start), count + " permissions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_SUBUNITS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEntities(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEntities();
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOG.info("[{}] entity permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_ENTITIES, elapsed(start), count + " permissions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_ENTITIES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEntityRoles(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEntityRoles();
            int count = response.roles() != null ? response.roles().size() : 0;
            LOG.info("[{}] entity roles: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_ENTITY_ROLES, elapsed(start), count + " roles"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_ENTITY_ROLES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQuerySubordinateRoles(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().querySubordinateRoles();
            int count = response.roles() != null ? response.roles().size() : 0;
            LOG.info("[{}] subordinate roles: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_SUBORDINATE_ROLES, elapsed(start), count + " roles"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_SUBORDINATE_ROLES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryAuthorizations(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityAuthorizationPermissionsQueryBuilder builder = EntityAuthorizationPermissionsQueryBuilder.granted();
            var response = context.client().permissions().queryAuthorizations(builder);
            int count = response.authorizationGrants() != null ? response.authorizationGrants().size() : 0;
            LOG.info("[{}] authorization permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_AUTHORIZATIONS, elapsed(start), count + " grants"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_AUTHORIZATIONS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEuEntities(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEuEntities(EuEntityPermissionsQueryBuilder.create());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOG.info("[{}] EU entity permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_EU_ENTITIES, elapsed(start), count + " permissions"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_EU_ENTITIES, elapsed(start), errorMessage(exception)));
        }
    }
}
