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

import io.github.mgrtomaszzurawski.ksef.client.model.PermissionsOperationResponseRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonPermissionsGrantRequestRaw;
import io.github.mgrtomaszzurawski.ksef.client.model.PersonalPermissionsQueryRequestRaw;
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

    @Override
    public String name() { return NAME; }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        // 1-2. Grant + operation status — skipped (requires proper subject identifiers)
        results.add(RunResult.skip(NAME, OP_GRANT_PERSON, "requires valid subject identifiers"));
        results.add(RunResult.skip(NAME, OP_GET_OP_STATUS, "depends on grant"));
        results.add(RunResult.skip(NAME, OP_REVOKE_COMMON, "depends on grant"));

        // 3. Query personal permissions
        runQueryPersonal(context, results);

        // 4. Query entity roles
        runQueryEntityRoles(context, results);

        // 5. Get attachment status
        runGetAttachmentStatus(context, results);

        return results;
    }

    private String runGrantPerson(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            PersonPermissionsGrantRequestRaw request = new PersonPermissionsGrantRequestRaw();
            PermissionsOperationResponseRaw response = context.client().permissions().grantPerson(request);
            String refNum = response.getReferenceNumber();
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
                    response.getStatus() != null ? response.getStatus().getCode() : "null");
            results.add(RunResult.ok(NAME, OP_GET_OP_STATUS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GET_OP_STATUS, elapsed(start), errorMessage(exception)));
        }
    }

    private void runQueryPersonal(DemoContext context, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            var response = context.client().permissions().queryPersonal(new PersonalPermissionsQueryRequestRaw());
            int count = response.getPermissions() != null ? response.getPermissions().size() : 0;
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
            int count = response.getRoles() != null ? response.getRoles().size() : 0;
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
            LOG.info("[{}] attachment allowed: {}", NAME, response.getIsAttachmentAllowed());
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
