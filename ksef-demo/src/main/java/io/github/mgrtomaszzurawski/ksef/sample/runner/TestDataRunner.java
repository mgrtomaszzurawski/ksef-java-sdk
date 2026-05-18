/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample.runner;

import io.github.mgrtomaszzurawski.ksef.sample.DemoContext;
import io.github.mgrtomaszzurawski.ksef.sample.report.RunResult;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefIdentifier;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.TestDataAdmin;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsGrantBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPermissionsRevokeBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestPersonCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestRateLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSessionLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectCreateBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.builder.TestSubjectLimitsBuilder;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectIdentifierType;
import io.github.mgrtomaszzurawski.ksef.sdk.domain.testdata.model.TestSubjectType;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.elapsed;
import static io.github.mgrtomaszzurawski.ksef.sample.runner.RunnerHelper.errorMessage;

/**
 * Runner for {@link TestDataAdmin} operations. Exercises every test-data endpoint
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataRunner.class);

    private static final String NAME = "testdata";

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

    private static final String SKIP_PROD_REASON = "Test data endpoints only available in test environments";
    private static final String SKIP_NOT_DEPLOYED_REASON =
            "TestData endpoints not deployed on this env (api-demo returns 404; they live on api-test)";
    private static final String SKIP_DEPENDS_ON_CREATE_SUBJECT = "depends on createSubject";
    private static final String SKIP_DEPENDS_ON_CREATE_PERSON = "depends on createPerson";
    private static final String SKIP_DEPENDS_ON_GRANT_PERMISSIONS = "depends on grantPermissions";
    private static final String SKIP_DEPENDS_ON_GRANT_ATTACHMENT = "depends on grantAttachment";
    private static final String SKIP_DEPENDS_ON_BLOCK_CONTEXT = "depends on blockContext";

    private static final String ENV_DEMO = "api-demo";
    private static final String ENV_TEST = "test";

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

    private static final int SESSION_MAX_INVOICE_SIZE_MB = 1;
    private static final int SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB = 4;
    private static final int SESSION_MAX_INVOICES = 100;

    private static final int SUBJECT_MAX_ENROLLMENTS = 10;
    private static final int SUBJECT_MAX_CERTIFICATES = 5;

    private static final int RATE_PER_SECOND = 5;
    private static final int RATE_PER_MINUTE = 50;
    private static final int RATE_PER_HOUR = 500;

    private static final String NIP_PREFIX = "nip=";

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

        TestDataAdmin testData = context.client().testData();

        // Probe first: testdata endpoints exist in OpenAPI spec but are not deployed to
        // every test environment (e.g. api-demo returns 404 — they live on api-test).
        // Bail out early with a clear skip message to avoid 8 noisy FAILs.
        if (!testDataEndpointsAvailable(testData)) {
            results.add(RunResult.skip(NAME, OP_CREATE_SUBJECT, SKIP_NOT_DEPLOYED_REASON));
            return results;
        }

        runSubjectPair(testData, results);

        runPersonPair(testData, results);

        runPermissionsPair(testData, context, results);

        runAttachmentPair(testData, results);

        runContextBlockPair(testData, results);

        runSetSessionLimits(testData, results);

        runSetSubjectLimits(testData, results);

        runSetRateLimits(testData, results);

        return results;
    }

    /**
     * Probe a single testdata endpoint to confirm the env exposes them.
     * Returns false if the first call returns 404 (route not deployed).
     */
    private static boolean testDataEndpointsAvailable(TestDataAdmin testData) {
        try {
            testData.removeSubject(KsefIdentifier.nip(generateTestNip()));
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

    private void runSubjectPair(TestDataAdmin testData, List<RunResult> results) {
        String subjectNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean createdOk = false;
        try {
            testData.createSubject(TestSubjectCreateBuilder.create(
                    subjectNip, TestSubjectType.JST, SUBJECT_DESCRIPTION).build());
            LOGGER.info("[{}] created test subject nip={}", NAME, subjectNip);
            results.add(RunResult.ok(NAME, OP_CREATE_SUBJECT, elapsed(start), NIP_PREFIX + subjectNip));
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
            testData.removeSubject(KsefIdentifier.nip(subjectNip));
            LOGGER.info("[{}] removed test subject nip={}", NAME, subjectNip);
            results.add(RunResult.ok(NAME, OP_REMOVE_SUBJECT, elapsed(removeStart), NIP_PREFIX + subjectNip));
        } catch (Exception exception) {
            LOGGER.warn("[{}] removeSubject failed for nip={} - test data left on server",
                    NAME, subjectNip);
            results.add(RunResult.fail(NAME, OP_REMOVE_SUBJECT, elapsed(removeStart),
                    errorMessage(exception)));
        }
    }

    private void runPersonPair(TestDataAdmin testData, List<RunResult> results) {
        String personNip = generateTestNip();
        String pesel = generateTestPesel();
        long start = System.currentTimeMillis();
        boolean createdOk = false;
        try {
            testData.createPerson(TestPersonCreateBuilder.create(
                    personNip, pesel, false, PERSON_DESCRIPTION).build());
            LOGGER.info("[{}] created test person nip={} pesel={}", NAME, personNip, pesel);
            results.add(RunResult.ok(NAME, OP_CREATE_PERSON, elapsed(start),
                    NIP_PREFIX + personNip + " pesel=" + pesel));
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
            testData.removePerson(KsefIdentifier.nip(personNip));
            LOGGER.info("[{}] removed test person nip={}", NAME, personNip);
            results.add(RunResult.ok(NAME, OP_REMOVE_PERSON, elapsed(removeStart), NIP_PREFIX + personNip));
        } catch (Exception exception) {
            LOGGER.warn("[{}] removePerson failed for nip={} - test data left on server",
                    NAME, personNip);
            results.add(RunResult.fail(NAME, OP_REMOVE_PERSON, elapsed(removeStart),
                    errorMessage(exception)));
        }
    }

    private void runPermissionsPair(TestDataAdmin testData, DemoContext context,
                                    List<RunResult> results) {
        String contextNip = context.nipIdentifier();
        String authorizedNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean grantedOk = false;
        try {
            testData.grantPermissions(TestPermissionsGrantBuilder.create(contextNip)
                    .authorizedNip(authorizedNip)
                    .invoiceRead()
                    .build());
            LOGGER.info("[{}] granted test permissions context={} authorized={}",
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
                    .authorizedNip(authorizedNip)
                    .build());
            LOGGER.info("[{}] revoked test permissions context={} authorized={}",
                    NAME, contextNip, authorizedNip);
            results.add(RunResult.ok(NAME, OP_REVOKE_PERMISSIONS, elapsed(revokeStart),
                    "authorized=" + authorizedNip));
        } catch (Exception exception) {
            LOGGER.warn("[{}] revokePermissions failed for authorized={} - test data left on server",
                    NAME, authorizedNip);
            results.add(RunResult.fail(NAME, OP_REVOKE_PERMISSIONS, elapsed(revokeStart),
                    errorMessage(exception)));
        }
    }

    private void runAttachmentPair(TestDataAdmin testData, List<RunResult> results) {
        String attachmentNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean grantedOk = false;
        try {
            testData.grantAttachment(KsefIdentifier.nip(attachmentNip));
            LOGGER.info("[{}] granted test attachment nip={}", NAME, attachmentNip);
            results.add(RunResult.ok(NAME, OP_GRANT_ATTACHMENT, elapsed(start), NIP_PREFIX + attachmentNip));
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
            testData.revokeAttachment(KsefIdentifier.nip(attachmentNip), java.time.LocalDate.now());
            LOGGER.info("[{}] revoked test attachment nip={}", NAME, attachmentNip);
            results.add(RunResult.ok(NAME, OP_REVOKE_ATTACHMENT, elapsed(revokeStart),
                    NIP_PREFIX + attachmentNip));
        } catch (Exception exception) {
            LOGGER.warn("[{}] revokeAttachment failed for nip={} - test data left on server",
                    NAME, attachmentNip);
            results.add(RunResult.fail(NAME, OP_REVOKE_ATTACHMENT, elapsed(revokeStart),
                    errorMessage(exception)));
        }
    }

    private void runContextBlockPair(TestDataAdmin testData, List<RunResult> results) {
        String blockNip = generateTestNip();
        long start = System.currentTimeMillis();
        boolean blockedOk = false;
        try {
            testData.blockContext(KsefIdentifier.nip(blockNip));
            LOGGER.info("[{}] blocked context nip={}", NAME, blockNip);
            results.add(RunResult.ok(NAME, OP_BLOCK_CONTEXT, elapsed(start), NIP_PREFIX + blockNip));
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
            testData.unblockContext(KsefIdentifier.nip(blockNip));
            LOGGER.info("[{}] unblocked context nip={}", NAME, blockNip);
            results.add(RunResult.ok(NAME, OP_UNBLOCK_CONTEXT, elapsed(unblockStart),
                    NIP_PREFIX + blockNip));
        } catch (Exception exception) {
            LOGGER.warn("[{}] unblockContext failed for nip={} - context still blocked on server",
                    NAME, blockNip);
            results.add(RunResult.fail(NAME, OP_UNBLOCK_CONTEXT, elapsed(unblockStart),
                    errorMessage(exception)));
        }
    }

    private void runSetSessionLimits(TestDataAdmin testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setSessionLimits(TestSessionLimitsBuilder.create()
                    .onlineSession(SESSION_MAX_INVOICE_SIZE_MB,
                            SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB,
                            SESSION_MAX_INVOICES)
                    .batchSession(SESSION_MAX_INVOICE_SIZE_MB,
                            SESSION_MAX_INVOICE_WITH_ATTACHMENT_MB,
                            SESSION_MAX_INVOICES)
                    .build());
            LOGGER.info("[{}] session limits applied", NAME);
            results.add(RunResult.ok(NAME, OP_SET_SESSION_LIMITS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SET_SESSION_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runSetSubjectLimits(TestDataAdmin testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setSubjectLimits(TestSubjectLimitsBuilder.create(TestSubjectIdentifierType.NIP)
                    .maxEnrollments(SUBJECT_MAX_ENROLLMENTS)
                    .maxCertificates(SUBJECT_MAX_CERTIFICATES)
                    .build());
            LOGGER.info("[{}] subject limits applied", NAME);
            results.add(RunResult.ok(NAME, OP_SET_SUBJECT_LIMITS, elapsed(start)));
        } catch (Exception exception) {
            results.add(RunResult.fail(NAME, OP_SET_SUBJECT_LIMITS, elapsed(start),
                    errorMessage(exception)));
        }
    }

    private void runSetRateLimits(TestDataAdmin testData, List<RunResult> results) {
        long start = System.currentTimeMillis();
        try {
            testData.setRateLimits(TestRateLimitsBuilder.create()
                    .invoiceSend(RATE_PER_SECOND, RATE_PER_MINUTE, RATE_PER_HOUR)
                    .build());
            LOGGER.info("[{}] rate limits applied", NAME);
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
        return lower.contains(ENV_DEMO) || lower.contains(ENV_TEST);
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
