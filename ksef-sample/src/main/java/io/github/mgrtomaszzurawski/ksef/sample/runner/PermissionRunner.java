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
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.PersonPermissionGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.model.builder.PersonalPermissionsQueryBuilder;
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Runner for PermissionClient operations. Grants a person permission, queries across
 * all query endpoints, checks statuses, then revokes — fully self-cleaning.
 */
public final class PermissionRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionRunner.class);
    private static final String NAME = "permission";
    private static final String OP_GRANT_PERSON = "grantPerson";
    private static final String OP_GET_OP_STATUS = "getOperationStatus";
    private static final String OP_QUERY_PERSONAL = "queryPersonal";
    private static final String OP_QUERY_ENTITY_ROLES = "queryEntityRoles";
    private static final String OP_GET_ATTACHMENT = "getAttachmentStatus";
    private static final String OP_REVOKE_COMMON = "revokeCommon";

    private static final String TEST_PERSON_PESEL = "82060411457";
    private static final String TEST_PERSON_FIRST_NAME = "Jan";
    private static final String TEST_PERSON_LAST_NAME = "Kowalski";
    private static final String GRANT_DESCRIPTION = "SDK Demo grant - will be revoked";

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1. Grant person permission
        String operationRef = runGrantPerson(context, results);

        // 2. Get operation status (if grant succeeded)
        if (operationRef != null) {
            runGetOperationStatus(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_GET_OP_STATUS, "depends on grant"));
        }

        // 3. Query personal permissions
        runQueryPersonal(context, results);

        // 4. Query entity roles
        runQueryEntityRoles(context, results);

        // 5. Get attachment status
        runGetAttachmentStatus(context, results);

        // 6. Revoke (cleanup)
        if (operationRef != null) {
            runRevokeCommon(context, operationRef, results);
        } else {
            results.add(RunResult.skip(NAME, OP_REVOKE_COMMON, "depends on grant"));
        }

        return results;
    }

    private String runGrantPerson(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionGrantBuilder grantBuilder = PersonPermissionGrantBuilder
                    .forPesel(TEST_PERSON_PESEL)
                    .description(GRANT_DESCRIPTION)
                    .personDetails(TEST_PERSON_FIRST_NAME, TEST_PERSON_LAST_NAME)
                    .invoiceRead();
            PermissionOperationResult response = context.client().permissions().grantPerson(grantBuilder);
            String refNum = response.referenceNumber();
            LOG.info("[{}] granted person permission, ref={}", NAME, refNum);
            results.add(RunResult.ok(NAME, OP_GRANT_PERSON, elapsed(start), "ref=" + refNum));
            return refNum;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_PERSON, elapsed(start), errorMessage(exception)));
            return null;
        }
    }

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
}
