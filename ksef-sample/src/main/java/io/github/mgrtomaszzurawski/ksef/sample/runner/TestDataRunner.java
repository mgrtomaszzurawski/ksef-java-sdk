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
import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.TestDataClient;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.model.TestDataIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.testdata.model.TestSubjectType;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for {@link TestDataClient} operations. Exercises every test-data endpoint
 * pair (create+remove, grant+revoke, block+unblock) and the one-shot limit setters.
 *
 * <p>This runner only operates against KSeF test environments
 * ({@code api-demo.ksef.mf.gov.pl}, preprod, etc.). In production it skips all
 * operations because the test-data endpoints do not exist there.</p>
 *
 * <p>All test data is created with random NIPs/PESEL values to avoid collisions
 * between runs and is removed immediately after creation. If creation succeeds and
 * the matching cleanup fails, the runner records both results independently and
 * logs a warning so the leftover record can be inspected.</p>
 */
public final class TestDataRunner implements DemoRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataRunner.class);

    private static final String NAME = "testdata";

    // --- Operation names ---
    private static final String OP_CREATE_SUBJECT = "createSubject";
    private static final String OP_REMOVE_SUBJECT = "removeSubject";
    private static final String OP_CREATE_PERSON = "createPerson";
    private static final String OP_REMOVE_PERSON = "removePerson";
    private static final String OP_GRANT_PERMISSIONS = "grantPermissions";
    private static final String OP_REVOKE_PERMISSIONS = "revokePermissions";
    private static final String OP_GRANT_ATTACHMENT = "grantAttachment";
    private static final String OP_REVOKE_ATTACHMENT = "revokeAttachment";
    private static final String OP_BLOCK_CONTEXT = "blockContext";
    private static final String OP_UNBLOCK_CONTEXT = "unblockContext";
    private static final String OP_SET_SESSION_LIMITS = "setSessionLimits";
    private static final String OP_SET_SUBJECT_LIMITS = "setSubjectLimits";
    private static final String OP_SET_RATE_LIMITS = "setRateLimits";

    // --- Skip reasons ---
    private static final String SKIP_PROD_REASON = "Test data endpoints only available in test environments";
    private static final String SKIP_NOT_DEPLOYED_REASON =
            "TestData endpoints not deployed on this env (api-demo returns 404; they live on api-test)";
    private static final String SKIP_DEPENDS_ON_CREATE_SUBJECT = "depends on createSubject";
    private static final String SKIP_DEPENDS_ON_CREATE_PERSON = "depends on createPerson";
    private static final String SKIP_DEPENDS_ON_GRANT_PERMISSIONS = "depends on grantPermissions";
    private static final String SKIP_DEPENDS_ON_GRANT_ATTACHMENT = "depends on grantAttachment";
    private static final String SKIP_DEPENDS_ON_BLOCK_CONTEXT = "depends on blockContext";

    // --- Environment markers (lower-cased substring match) ---
    private static final String ENV_DEMO = "api-demo";
    private static final String ENV_TEST = "test";
    private static final String ENV_PREPROD = "preprod";

    // --- Test data values ---
    private static final String SUBJECT_DESCRIPTION = "SDK Demo test subject - will be removed";
    private static final String PERSON_DESCRIPTION = "SDK Demo test person - will be removed";
    private static final int NIP_DIGITS = 10;
    private static final int PESEL_DIGITS = 11;
    private static final int RADIX_DECIMAL = 10;
    private static final int[] NIP_WEIGHTS = {6, 5, 7, 2, 3, 4, 5, 6, 7};
    private static final int[] PESEL_WEIGHTS = {1, 3, 7, 9, 1, 3, 7, 9, 1, 3};
    private static final int CHECKSUM_MOD = 11;
    private static final int CHECKSUM_INVALID = 10;
    private static final int LEADING_DIGIT_MIN = 1;
    private static final int LEADING_DIGIT_MAX = 9;

    // --- Session limits values (small reasonable defaults) ---
    private static final int SESSION_MAX_INVOICE_SIZE_MB = 1;
    private static final int SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB = 4;
    private static final int SESSION_MAX_INVOICES = 100;

    // --- Subject limits values ---
    private static final int SUBJECT_MAX_ENROLLMENTS = 10;
    private static final int SUBJECT_MAX_CERTIFICATES = 5;

    // --- Rate limits values ---
    private static final int RATE_PER_SECOND = 5;
    private static final int RATE_PER_MINUTE = 50;
    private static final int RATE_PER_HOUR = 500;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<RunResult> run(DemoContext context) {
        List<RunResult> results = new ArrayList<>();

        if (!isTestEnvironment(context.environment())) {
            results.add(RunResult.skip(NAME, OP_CREATE_SUBJECT, SKIP_PROD_REASON));
            return results;
        }

        TestDataClient testData = context.client().testData();

        // Probe first: testdata endpoints exist in OpenAPI spec but are not deployed to
        // every test environment (e.g. api-demo returns 404 — they live on api-test).
        // Bail out early with a clear skip message to avoid 8 noisy FAILs.
        if (!testDataEndpointsAvailable(testData)) {
            results.add(RunResult.skip(NAME, OP_CREATE_SUBJECT, SKIP_NOT_DEPLOYED_REASON));
            return results;
        }

        // 1. createSubject + removeSubject
        runSubjectPair(testData, results);

        // 2. createPerson + removePerson
        runPersonPair(testData, results);

        // 3. grantPermissions + revokePermissions
        runPermissionsPair(testData, context, results);

        // 4. grantAttachment + revokeAttachment
        runAttachmentPair(testData, results);

        // 5. blockContext + unblockContext
        runContextBlockPair(testData, results);

        // 6. setSessionLimits (no undo endpoint per task spec)
        runSetSessionLimits(testData, results);

        // 7. setSubjectLimits
        runSetSubjectLimits(testData, results);

        // 8. setRateLimits
        runSetRateLimits(testData, results);

        return results;
    }

    /**
     * Probe a single testdata endpoint to confirm the env exposes them.
     * Returns false if the first call returns 404 (route not deployed).
     */
    private static boolean testDataEndpointsAvailable(TestDataClient testData) {
        try {
            testData.removeSubject(generateTestNip());
            // 200/204 — endpoint exists (probably no-op for nonexistent NIP)
            return true;
        } catch (io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefNotFoundException notFound) {
            // 404 → endpoints not deployed on this env
            return false;
        } catch (Exception other) {
            // Any other error means the endpoint exists but rejected our probe payload.
            // We treat that as "available — proceed to real ops which will produce
            // their own per-op results".
            return true;
        }
    }

    private void runSubjectPair(TestDataClient testData, List<RunResult> results) {
        String subjectNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean createdOk = false;
        try {
            testData.createSubject(TestSubjectCreateBuilder.create(
                    subjectNip, TestSubjectType.JST, SUBJECT_DESCRIPTION));
            LOG.info("[{}] created test subject nip={}", NAME, subjectNip);
            results.add(RunResult.ok(NAME, OP_CREATE_SUBJECT, elapsed(start), "nip=" + subjectNip));
            createdOk = true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CREATE_SUBJECT, elapsed(start), errorMessage(exception)));
        }

        if (!createdOk) {
            results.add(RunResult.skip(NAME, OP_REMOVE_SUBJECT, SKIP_DEPENDS_ON_CREATE_SUBJECT));
            return;
        }

        long removeStart = System.currentTimeMillis();
        try {
            testData.removeSubject(subjectNip);
            LOG.info("[{}] removed test subject nip={}", NAME, subjectNip);
            results.add(RunResult.ok(NAME, OP_REMOVE_SUBJECT, elapsed(removeStart), "nip=" + subjectNip));
        } catch (Exception exception) {
            LOG.warn("[{}] removeSubject failed for nip={} - test data left on server",
                    NAME, subjectNip);
            results.add(RunResult.fail(NAME, OP_REMOVE_SUBJECT, elapsed(removeStart),
                    errorMessage(exception)));
        }
    }

    private void runPersonPair(TestDataClient testData, List<RunResult> results) {
        String personNip = generateTestNip();
        String pesel = generateTestPesel();
        long start = System.currentTimeMillis();
        boolean createdOk = false;
        try {
            testData.createPerson(TestPersonCreateBuilder.create(
                    personNip, pesel, false, PERSON_DESCRIPTION));
            LOG.info("[{}] created test person nip={} pesel={}", NAME, personNip, pesel);
            results.add(RunResult.ok(NAME, OP_CREATE_PERSON, elapsed(start),
                    "nip=" + personNip + " pesel=" + pesel));
            createdOk = true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_CREATE_PERSON, elapsed(start), errorMessage(exception)));
        }

        if (!createdOk) {
            results.add(RunResult.skip(NAME, OP_REMOVE_PERSON, SKIP_DEPENDS_ON_CREATE_PERSON));
            return;
        }

        long removeStart = System.currentTimeMillis();
        try {
            testData.removePerson(personNip);
            LOG.info("[{}] removed test person nip={}", NAME, personNip);
            results.add(RunResult.ok(NAME, OP_REMOVE_PERSON, elapsed(removeStart), "nip=" + personNip));
        } catch (Exception exception) {
            LOG.warn("[{}] removePerson failed for nip={} - test data left on server",
                    NAME, personNip);
            results.add(RunResult.fail(NAME, OP_REMOVE_PERSON, elapsed(removeStart),
                    errorMessage(exception)));
        }
    }

    private void runPermissionsPair(TestDataClient testData, DemoContext context,
                                    List<RunResult> results) {
        String contextNip = context.nipIdentifier();
        String authorizedNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean grantedOk = false;
        try {
            testData.grantPermissions(TestPermissionsGrantBuilder.create(contextNip)
                    .authorizedNip(authorizedNip)
                    .invoiceRead());
            LOG.info("[{}] granted test permissions context={} authorized={}",
                    NAME, contextNip, authorizedNip);
            results.add(RunResult.ok(NAME, OP_GRANT_PERMISSIONS, elapsed(start),
                    "authorized=" + authorizedNip));
            grantedOk = true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_PERMISSIONS, elapsed(start),
                    errorMessage(exception)));
        }

        if (!grantedOk) {
            results.add(RunResult.skip(NAME, OP_REVOKE_PERMISSIONS, SKIP_DEPENDS_ON_GRANT_PERMISSIONS));
            return;
        }

        long revokeStart = System.currentTimeMillis();
        try {
            testData.revokePermissions(TestPermissionsRevokeBuilder.create(contextNip)
                    .authorizedNip(authorizedNip));
            LOG.info("[{}] revoked test permissions context={} authorized={}",
                    NAME, contextNip, authorizedNip);
            results.add(RunResult.ok(NAME, OP_REVOKE_PERMISSIONS, elapsed(revokeStart),
                    "authorized=" + authorizedNip));
        } catch (Exception exception) {
            LOG.warn("[{}] revokePermissions failed for authorized={} - test data left on server",
                    NAME, authorizedNip);
            results.add(RunResult.fail(NAME, OP_REVOKE_PERMISSIONS, elapsed(revokeStart),
                    errorMessage(exception)));
        }
    }

    private void runAttachmentPair(TestDataClient testData, List<RunResult> results) {
        String attachmentNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean grantedOk = false;
        try {
            testData.grantAttachment(attachmentNip);
            LOG.info("[{}] granted test attachment nip={}", NAME, attachmentNip);
            results.add(RunResult.ok(NAME, OP_GRANT_ATTACHMENT, elapsed(start), "nip=" + attachmentNip));
            grantedOk = true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_GRANT_ATTACHMENT, elapsed(start),
                    errorMessage(exception)));
        }

        if (!grantedOk) {
            results.add(RunResult.skip(NAME, OP_REVOKE_ATTACHMENT, SKIP_DEPENDS_ON_GRANT_ATTACHMENT));
            return;
        }

        long revokeStart = System.currentTimeMillis();
        try {
            testData.revokeAttachment(attachmentNip);
            LOG.info("[{}] revoked test attachment nip={}", NAME, attachmentNip);
            results.add(RunResult.ok(NAME, OP_REVOKE_ATTACHMENT, elapsed(revokeStart),
                    "nip=" + attachmentNip));
        } catch (Exception exception) {
            LOG.warn("[{}] revokeAttachment failed for nip={} - test data left on server",
                    NAME, attachmentNip);
            results.add(RunResult.fail(NAME, OP_REVOKE_ATTACHMENT, elapsed(revokeStart),
                    errorMessage(exception)));
        }
    }

    private void runContextBlockPair(TestDataClient testData, List<RunResult> results) {
        String blockNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean blockedOk = false;
        try {
            testData.blockContext(TestDataIdentifierType.NIP, blockNip);
            LOG.info("[{}] blocked context nip={}", NAME, blockNip);
            results.add(RunResult.ok(NAME, OP_BLOCK_CONTEXT, elapsed(start), "nip=" + blockNip));
            blockedOk = true;
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_BLOCK_CONTEXT, elapsed(start),
                    errorMessage(exception)));
        }

        if (!blockedOk) {
            results.add(RunResult.skip(NAME, OP_UNBLOCK_CONTEXT, SKIP_DEPENDS_ON_BLOCK_CONTEXT));
            return;
        }

        long unblockStart = System.currentTimeMillis();
        try {
            testData.unblockContext(TestDataIdentifierType.NIP, blockNip);
            LOG.info("[{}] unblocked context nip={}", NAME, blockNip);
            results.add(RunResult.ok(NAME, OP_UNBLOCK_CONTEXT, elapsed(unblockStart),
                    "nip=" + blockNip));
        } catch (Exception exception) {
            LOG.warn("[{}] unblockContext failed for nip={} - context still blocked on server",
                    NAME, blockNip);
            results.add(RunResult.fail(NAME, OP_UNBLOCK_CONTEXT, elapsed(unblockStart),
                    errorMessage(exception)));
        }
    }

    private void runSetSessionLimits(TestDataClient testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setSessionLimits(TestSessionLimitsBuilder.create()
                    .onlineSession(SESSION_MAX_INVOICE_SIZE_MB,
                            SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB,
                            SESSION_MAX_INVOICES)
                    .batchSession(SESSION_MAX_INVOICE_SIZE_MB,
                            SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB,
                            SESSION_MAX_INVOICES));
            LOG.info("[{}] session limits applied", NAME);
            results.add(RunResult.ok(NAME, OP_SET_SESSION_LIMITS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SET_SESSION_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runSetSubjectLimits(TestDataClient testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setSubjectLimits(TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                    .maxEnrollments(SUBJECT_MAX_ENROLLMENTS)
                    .maxCertificates(SUBJECT_MAX_CERTIFICATES));
            LOG.info("[{}] subject limits applied", NAME);
            results.add(RunResult.ok(NAME, OP_SET_SUBJECT_LIMITS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SET_SUBJECT_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runSetRateLimits(TestDataClient testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setRateLimits(TestRateLimitsBuilder.create()
                    .invoiceSend(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR));
            LOG.info("[{}] rate limits applied", NAME);
            results.add(RunResult.ok(NAME, OP_SET_RATE_LIMITS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SET_RATE_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private static boolean isTestEnvironment(String environment) {
        if (environment == null) {
            return false;
        }
        String lower = environment.toLowerCase(Locale.ROOT);
        return lower.contains(ENV_DEMO) || lower.contains(ENV_TEST) || lower.contains(ENV_PREPROD);
    }

    /**
     * Generate a 10-digit Polish NIP with a valid checksum.
     * Uses a random non-zero leading digit to avoid leading-zero parsing issues
     * and recomputes on the rare occasion that the checksum lands on the invalid value 10.
     */
    private static String generateTestNip() {
        while (true) {
            int[] digits = new int[NIP_DIGITS];
            digits[0] = LEADING_DIGIT_MIN
                    + RANDOM.nextInt(LEADING_DIGIT_MAX - LEADING_DIGIT_MIN + 1);
            for (int index = 1; index < NIP_DIGITS - 1; index++) {
                digits[index] = RANDOM.nextInt(RADIX_DECIMAL);
            }
            int sum = 0;
            for (int index = 0; index < NIP_WEIGHTS.length; index++) {
                sum += digits[index] * NIP_WEIGHTS[index];
            }
            int checksum = sum % CHECKSUM_MOD;
            if (checksum == CHECKSUM_INVALID) {
                continue;
            }
            digits[NIP_DIGITS - 1] = checksum;
            return digitsToString(digits);
        }
    }

    /**
     * Generate an 11-digit Polish PESEL with a valid checksum.
     * The first six digits encode birth date; for test data any plausible date works
     * — we use a fixed safe pattern (year 92, month 06, day 04 → "920604") and randomise
     * the remaining four digits before the checksum.
     */
    private static String generateTestPesel() {
        int[] digits = {9, 2, 0, 6, 0, 4, 0, 0, 0, 0, 0};
        for (int index = 6; index < PESEL_DIGITS - 1; index++) {
            digits[index] = RANDOM.nextInt(RADIX_DECIMAL);
        }
        int sum = 0;
        for (int index = 0; index < PESEL_WEIGHTS.length; index++) {
            sum += digits[index] * PESEL_WEIGHTS[index];
        }
        int checksum = (RADIX_DECIMAL - (sum % RADIX_DECIMAL)) % RADIX_DECIMAL;
        digits[PESEL_DIGITS - 1] = checksum;
        return digitsToString(digits);
    }

    private static String digitsToString(int[] digits) {
        StringBuilder builder = new StringBuilder(digits.length);
        for (int digit : digits) {
            builder.append(digit);
        }
        return builder.toString();
    }
}
