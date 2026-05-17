/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityAuthorizationPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityAdminPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.EuEntityPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.IndirectPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubordinateEntityRolesQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.builder.SubunitPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model.PermissionOperationStatus;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for Permissions operations. Exercises every grant variant (person, entity,
 * authorization, indirect, subunit, EU entity admin, EU entity), every query variant
 * (personal, persons, subunits, entities, entity-roles, subordinate-roles, authorizations,
 * EU entities), both revoke variants, and the operation/attachment status endpoints.
 * <p>
 * Each grant is wrapped grant -> status -> revoke for self-cleanup. Failures are reported
 * per-operation; one failing call never short-circuits the rest.
 */
public final class PermissionRunner implements DemoRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRunner.class);
    private static final String NAME = "permission";
    private static final String SUFFIX_PERMISSIONS = " permissions";

    private static final String OP_GRANT_PERSON = "grantPerson";
    private static final String OP_GRANT_ENTITY = "grantEntity";
    private static final String OP_GRANT_AUTHORIZATION = "grantAuthorization";
    private static final String OP_GRANT_INDIRECT = "grantIndirect";
    private static final String OP_GRANT_SUBUNIT = "grantSubunit";
    private static final String OP_GRANT_EU_ENTITY_ADMIN = "grantEuEntityAdmin";
    private static final String OP_GRANT_EU_ENTITY = "grantEuEntity";
    private static final String OP_QUERY_PERSONAL = "queryPersonal";
    private static final String OP_QUERY_PERSONS = "queryPersons";
    private static final String OP_QUERY_SUBUNITS = "querySubunits";
    private static final String OP_QUERY_ENTITIES = "queryEntities";
    private static final String OP_QUERY_ENTITY_ROLES = "queryEntityRoles";
    private static final String OP_QUERY_SUBORDINATE_ROLES = "querySubordinateRoles";
    private static final String OP_QUERY_AUTHORIZATIONS = "queryAuthorizations";
    private static final String OP_QUERY_EU_ENTITIES = "queryEuEntities";
    private static final String OP_GET_ATTACHMENT = "getAttachmentStatus";
    // OP_REVOKE_* dropped — runner no longer attempts revoke without a queried permissionId.

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
    private static final String TEST_EU_VAT_UE_SUFFIX = "-DE123456789";
    private static final String TEST_EU_ENTITY_NAME = "EU Partner GmbH";
    private static final String TEST_EU_ENTITY_ADDRESS = "Berlin, Germany";

    private static final String GRANT_PERSON_DESC = "SDK Demo person grant - will be revoked";
    private static final String GRANT_ENTITY_DESC = "SDK Demo entity grant - will be revoked";
    private static final String GRANT_AUTH_DESC = "SDK Demo authorization grant - will be revoked";
    private static final String GRANT_INDIRECT_DESC = "SDK Demo indirect grant - will be revoked";
    private static final String GRANT_SUBUNIT_DESC = "SDK Demo subunit grant - will be revoked";
    private static final String GRANT_EU_ADMIN_DESC = "SDK Demo EU admin grant - will be revoked";
    private static final String GRANT_EU_ENTITY_DESC = "SDK Demo EU entity grant - will be revoked";

    private static final String SKIP_REQUIRES_NIP_VAT_UE =
            "requires NipVatUe-context credentials (current creds are NIP-context)";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        runPersonGrantCycle(context, results);
        runEntityGrantCycle(context, results);
        runAuthorizationGrantCycle(context, results);
        runIndirectGrantCycle(context, results);
        runSubunitGrantCycle(context, results);
        runEuEntityAdminGrantCycle(context, results);
        runEuEntityGrantCycle(context, results);

        runQueryPersonal(context, results);
        runQueryPersons(context, results);
        runQuerySubunits(context, results);
        runQueryEntities(context, results);
        runQueryEntityRoles(context, results);
        runQuerySubordinateRoles(context, results);
        runQueryAuthorizations(context, results);
        runQueryEuEntities(context, results);

        runGetAttachmentStatus(context, results);

        return results;
    }

    private void runPersonGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantPerson(context, results);
        // Revoke not chained — ADR-032 sync grant returns terminal status only,
        // not the permission ID. Real consumers query permissions() to find the
        // grant just created and pass that ID to revoke.
    }

    private void runEntityGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantEntity(context, results);
    }

    private void runAuthorizationGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantAuthorization(context, results);
    }

    private void runIndirectGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantIndirect(context, results);
    }

    private void runSubunitGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantSubunit(context, results);
    }

    private void runEuEntityAdminGrantCycle(DemoContext context, List<RunResult> results) {
        runGrantEuEntityAdmin(context, results);
    }

    private void runEuEntityGrantCycle(DemoContext context, List<RunResult> results) {
        if (context.identifierType() != KsefIdentifier.Type.NIP_VAT_UE) {
            results.add(RunResult.skip(NAME, OP_GRANT_EU_ENTITY, SKIP_REQUIRES_NIP_VAT_UE));
            return;
        }
        runGrantEuEntity(context, results);
    }

    private void runGrantPerson(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionGrantBuilder builder = PersonPermissionGrantBuilder
                    .forPesel(TEST_PERSON_PESEL)
                    .description(GRANT_PERSON_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME)
                    .invoiceRead();
            PermissionOperationStatus response = context.client().permissions().grantPerson(builder.build());
            LOGGER.info("[{}] grantPerson terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_PERSON, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_PERSON, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantEntity(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityPermissionGrantBuilder builder = EntityPermissionGrantBuilder
                    .forNip(TEST_ENTITY_NIP)
                    .description(GRANT_ENTITY_DESC)
                    .entityDetails(TEST_ENTITY_FULL_NAME)
                    .invoiceRead();
            PermissionOperationStatus response = context.client().permissions().grantEntity(builder.build());
            LOGGER.info("[{}] grantEntity terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_ENTITY, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_ENTITY, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantAuthorization(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityAuthorizationPermissionGrantBuilder builder = EntityAuthorizationPermissionGrantBuilder
                    .forNip(TEST_AUTHORIZATION_NIP)
                    .description(GRANT_AUTH_DESC)
                    .entityDetails(TEST_AUTHORIZATION_NAME)
                    .selfInvoicing();
            PermissionOperationStatus response = context.client().permissions().grantAuthorization(builder.build());
            LOGGER.info("[{}] grantAuthorization terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_AUTHORIZATION, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_AUTHORIZATION, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantIndirect(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            IndirectPermissionGrantBuilder builder = IndirectPermissionGrantBuilder
                    .forNip(TEST_INDIRECT_NIP)
                    .description(GRANT_INDIRECT_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME)
                    .invoiceRead();
            PermissionOperationStatus response = context.client().permissions().grantIndirect(builder.build());
            LOGGER.info("[{}] grantIndirect terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_INDIRECT, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_INDIRECT, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantSubunit(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            SubunitPermissionGrantBuilder builder = SubunitPermissionGrantBuilder
                    .forPesel(TEST_PERSON_PESEL)
                    .contextNip(TEST_SUBUNIT_CONTEXT_NIP)
                    .description(GRANT_SUBUNIT_DESC)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME);
            PermissionOperationStatus response = context.client().permissions().grantSubunit(builder.build());
            LOGGER.info("[{}] grantSubunit terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_SUBUNIT, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_SUBUNIT, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantEuEntityAdmin(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EuEntityAdminPermissionGrantBuilder builder = EuEntityAdminPermissionGrantBuilder
                    .forFingerprint(TEST_FINGERPRINT)
                    .contextNipVatUe(context.nipIdentifier() + TEST_EU_VAT_UE_SUFFIX)
                    .description(GRANT_EU_ADMIN_DESC)
                    .euEntityName(TEST_EU_ENTITY_NAME)
                    .subjectEntityByFingerprint(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS)
                    .euEntityDetails(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS);
            PermissionOperationStatus response = context.client().permissions().grantEuEntityAdmin(builder.build());
            LOGGER.info("[{}] grantEuEntityAdmin terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_EU_ENTITY_ADMIN, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_EU_ENTITY_ADMIN, elapsed(start), errorMessage(exception)));
        }
    }

    private void runGrantEuEntity(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EuEntityPermissionGrantBuilder builder = EuEntityPermissionGrantBuilder
                    .forFingerprint(TEST_FINGERPRINT)
                    .description(GRANT_EU_ENTITY_DESC)
                    .subjectEntityByFingerprint(TEST_EU_ENTITY_NAME, TEST_EU_ENTITY_ADDRESS)
                    .invoiceRead();
            PermissionOperationStatus response = context.client().permissions().grantEuEntity(builder.build());
            LOGGER.info("[{}] grantEuEntity terminal code={}", NAME, terminalCode(response));
            results.add(RunResult.ok(NAME, OP_GRANT_EU_ENTITY, elapsed(start), "code=" + terminalCode(response)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_EU_ENTITY, elapsed(start), errorMessage(exception)));
        }
    }

    private static String terminalCode(PermissionOperationStatus status) {
        return status.status() == null ? "null" : Integer.toString(status.status().code());
    }

    private void runGetAttachmentStatus(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().getAttachmentStatus();
            LOGGER.info("[{}] attachment allowed: {}", NAME, response.attachmentAllowed());
            results.add(RunResult.ok(NAME, OP_GET_ATTACHMENT, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_ATTACHMENT, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryPersonal(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryPersonal(PersonalPermissionsQueryBuilder.create().build());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOGGER.info("[{}] personal permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_PERSONAL, elapsed(start), count + SUFFIX_PERMISSIONS));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PERSONAL, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryPersons(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionsQueryBuilder builder = PersonPermissionsQueryBuilder
                    .permissionsGrantedInCurrentContext();
            var response = context.client().permissions().queryPersons(builder.build());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOGGER.info("[{}] person permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_PERSONS, elapsed(start), count + SUFFIX_PERMISSIONS));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_PERSONS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQuerySubunits(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().querySubunits(SubunitPermissionsQueryBuilder.create().build());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOGGER.info("[{}] subunit permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_SUBUNITS, elapsed(start), count + SUFFIX_PERMISSIONS));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_SUBUNITS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEntities(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEntities(EntityPermissionsQueryBuilder.create().build());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOGGER.info("[{}] entity permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_ENTITIES, elapsed(start), count + SUFFIX_PERMISSIONS));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_ENTITIES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEntityRoles(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEntityRoles(EntityRolesQueryBuilder.create().build());
            int count = response.roles() != null ? response.roles().size() : 0;
            LOGGER.info("[{}] entity roles: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_ENTITY_ROLES, elapsed(start), count + " roles"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_ENTITY_ROLES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQuerySubordinateRoles(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().querySubordinateRoles(SubordinateEntityRolesQueryBuilder.create().build());
            int count = response.roles() != null ? response.roles().size() : 0;
            LOGGER.info("[{}] subordinate roles: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_SUBORDINATE_ROLES, elapsed(start), count + " roles"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_SUBORDINATE_ROLES, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryAuthorizations(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            EntityAuthorizationPermissionsQueryBuilder builder = EntityAuthorizationPermissionsQueryBuilder.granted();
            var response = context.client().permissions().queryAuthorizations(builder.build());
            int count = response.authorizationGrants() != null ? response.authorizationGrants().size() : 0;
            LOGGER.info("[{}] authorization permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_AUTHORIZATIONS, elapsed(start), count + " grants"));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_AUTHORIZATIONS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryEuEntities(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryEuEntities(EuEntityPermissionsQueryBuilder.create().build());
            int count = response.permissions() != null ? response.permissions().size() : 0;
            LOGGER.info("[{}] EU entity permissions: {} found", NAME, count);
            results.add(RunResult.ok(NAME, OP_QUERY_EU_ENTITIES, elapsed(start), count + SUFFIX_PERMISSIONS));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_QUERY_EU_ENTITIES, elapsed(start), errorMessage(exception)));
        }
    }
}
