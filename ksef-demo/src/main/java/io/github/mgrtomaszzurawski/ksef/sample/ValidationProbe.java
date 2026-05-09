/*
 * KSeF Demo App - Demo application exercising the KSeF Java SDK against the live demo server
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
package io.github.mgrtomaszzurawski.ksef.sample;

import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.common.ApiPaths;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.config.KsefTokenCredentials;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation probe — sends invalid requests to KSeF endpoints and logs
 * server error responses. Output goes via SLF4J as markdown-formatted
 * validation results so the operator can capture
 * {@code mvn exec:java -pl ksef-demo} stdout into a report file.
 */
public final class ValidationProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationProbe.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int DETAILS_MAX_LENGTH = 200;
    private static final int SHORT_SUMMARY_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 40;
    private static final int TRUNCATE_ELLIPSIS_RESERVE = 3;
    private static final int EXCEPTION_CODE_OFFSET = 16;
    private static final int EXCEPTION_CODE_MAX_OFFSET = 22;
    private static final int TEST_LONG_STRING_LENGTH = 500;
    private static final int TEST_LONG_DESCRIPTION_LENGTH = 300;
    private static final int TEST_LONG_REF_LENGTH = 200;

    private static final String DETAILS_FIELD_PREFIX = "\"details\":[";
    private static final String EXCEPTION_CODE_FIELD_PREFIX = "\"exceptionCode\":";
    private static final String ELLIPSIS = "...";
    private static final String DASH = "-";

    private static final String CREDENTIALS_FILE = "ksef-credentials.properties";
    private static final String PROP_ENVIRONMENT = "ksef.environment";
    private static final String PROP_TOKEN = "ksef.token";
    private static final String PROP_NIP = "ksef.nip";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_DELETE = "DELETE";

    private static final String LOG_PROBE_HEADER = "# KSeF Server-Side Validation Probe Results";
    private static final String LOG_GENERATED_AT = "Generated: {}";
    private static final String LOG_TABLE_HEADER = "| Input | HTTP | Code | Details |";
    private static final String LOG_TABLE_DIVIDER = "|-------|------|------|---------|";
    private static final String LOG_TABLE_ROW = "| {} | {} | {} | {} |";
    private static final String LOG_TABLE_ERROR_ROW = "| {} | ERR | - | {} |";

    private static final String NON_DIGIT_REGEX = "\\D";
    private static final String EMPTY = "";

    private static final String BODY_EMPTY_OBJECT = "{}";
    private static final String DESC_EMPTY_BODY = "Empty body";
    private static final String DESC_NULL_REQUIRED = "Null required fields";
    private static final String DESC_VALID_REQUEST = "Valid request";
    private static final String DESC_INVALID_REF = "Invalid reference number";

    private static final String PATH_AUTH_CHALLENGE = ApiPaths.AUTH + "/challenge";
    private static final String PATH_INVOICES_QUERY_METADATA = ApiPaths.INVOICES + "/query/metadata";

    private final String baseUrl;
    private final String bearerToken;
    private final HttpClient httpClient;

    private ValidationProbe(String baseUrl, String bearerToken) {
        this.baseUrl = baseUrl;
        this.bearerToken = bearerToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (FileInputStream credentialsFile = new FileInputStream(CREDENTIALS_FILE)) {
            props.load(credentialsFile);
        }

        String ksefUrl = props.getProperty(PROP_ENVIRONMENT);
        String ksefToken = props.getProperty(PROP_TOKEN);
        String nipIdentifier = props.getProperty(PROP_NIP);

        LOGGER.info("=== KSeF Validation Probe ===");
        LOGGER.info("Environment: {}", ksefUrl);
        LOGGER.debug("NIP: {}", nipIdentifier);

        try (KsefClient client = KsefClient.builder().environment(KsefEnvironment.custom(ksefUrl))
                .credentials(new KsefTokenCredentials(ksefToken, nipIdentifier))
                .build()) {
            // Drive lazy auth via any authenticated read.
            client.auth().streamSessions().findAny();
            LOGGER.info("Authenticated successfully");

            String bearer = extractBearerToken(client);

            ValidationProbe probe = new ValidationProbe(ksefUrl, bearer);
            probe.runAllProbes();

            client.auth().terminate();
            LOGGER.info("Session terminated. Probe complete.");
        }
    }

    /**
     * Diagnostic-only — reach into {@code KsefClient.sessionContext} to
     * pull the active bearer token for raw out-of-SDK HTTP probes. The
     * SDK no longer exposes {@code bearerToken()} as a public API
     * (PR6) — auth state is internal — but the probe still needs a
     * valid token to test endpoints with the SDK out of the picture.
     */
    private static String extractBearerToken(KsefClient client) {
        try {
            java.lang.reflect.Field field = KsefClient.class.getDeclaredField("sessionContext");
            field.setAccessible(true);
            Object sessionContext = field.get(client);
            return (String) sessionContext.getClass().getMethod("token").invoke(sessionContext);
        } catch (ReflectiveOperationException reflectiveFailure) {
            throw new IllegalStateException(
                    "Probe could not read bearer token from KsefClient internals", reflectiveFailure);
        }
    }

    private void runAllProbes() {
        LOGGER.info(LOG_PROBE_HEADER);
        LOGGER.info(LOG_GENERATED_AT, OffsetDateTime.now());

        probeUnauthEndpoints();
        probeAuthEndpoints();
    }

    private void probeUnauthEndpoints() {
        LOGGER.info("## Unauthenticated Endpoints");

        probeEndpoint(METHOD_GET, "/api/v2/security/public-key-certificates", null, false,
                "No body needed, always returns certificates");

        probeEndpoint(METHOD_POST, PATH_AUTH_CHALLENGE, null, false,
                "No body needed");
        probeEndpoint(METHOD_POST, PATH_AUTH_CHALLENGE, "{}", false,
                "Empty body accepted");
        probeEndpoint(METHOD_POST, PATH_AUTH_CHALLENGE, "{\"foo\":\"bar\"}", false,
                "Unknown fields ignored");

        LOGGER.info("### POST /api/v2/auth/ksef-token");
        probeAndLog("/api/v2/auth/ksef-token", false, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"challenge\":null,\"contextIdentifier\":null,\"encryptedToken\":null}", DESC_NULL_REQUIRED},
            {"{\"challenge\":\"\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"\"},\"encryptedToken\":\"\"}", "Empty strings"},
            {"{\"challenge\":123}", "Wrong type (int for string)"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"INVALID\",\"value\":\"1234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "Invalid enum value"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"123\"},\"encryptedToken\":\"dGVzdA==\"}", "Invalid NIP (too short)"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"12345678901234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "NIP too long"},
            {"{\"challenge\":\"" + "A".repeat(TEST_LONG_STRING_LENGTH) + "\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"1234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "Very long challenge (500 chars)"},
        });
    }

    private void probeAuthEndpoints() {
        LOGGER.info("## Authenticated Endpoints");

        LOGGER.info("### POST /api/v2/invoices/query/metadata");
        probeAndLog(PATH_INVOICES_QUERY_METADATA, true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"subjectType\":null}", "Null subjectType"},
            {"{\"subjectType\":\"INVALID\"}", "Invalid enum"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":null}", "Null dateRange"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":null,\"from\":null}}", "Null nested fields"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"not-a-date\"}}", "Invalid date format"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":0}", "pageSize=0"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":-1}", "pageSize=-1"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":999999}", "pageSize=999999"},
        });

        LOGGER.info("### POST /api/v2/tokens");
        probeAndLog("/api/v2/tokens", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"description\":null}", "Null description"},
            {"{\"description\":\"\"}", "Empty description"},
            {"{\"description\":\"test\",\"requestedPermissions\":null}", "Null permissions"},
            {"{\"description\":\"test\",\"requestedPermissions\":[]}", "Empty permissions array"},
            {"{\"description\":\"test\",\"requestedPermissions\":[\"INVALID\"]}", "Invalid permission type"},
            {"{\"description\":\"" + "A".repeat(TEST_LONG_DESCRIPTION_LENGTH) + "\",\"requestedPermissions\":[\"InvoiceRead\"]}", "Very long description (300 chars)"},
        });

        LOGGER.info("### GET /api/v2/limits/context");
        probeEndpoint(METHOD_GET, "/api/v2/limits/context", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### GET /api/v2/limits/subject");
        probeEndpoint(METHOD_GET, "/api/v2/limits/subject", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### GET /api/v2/rate-limits");
        probeEndpoint(METHOD_GET, "/api/v2/rate-limits", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### POST /api/v2/permissions/persons/grants");
        probeAndLog("/api/v2/permissions/persons/grants", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"permission\":null,\"subjectIdentifier\":null}", DESC_NULL_REQUIRED},
            {"{\"permission\":\"INVALID\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"12345678901\"},\"description\":\"test\"}", "Invalid permission enum"},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"123\"},\"description\":\"test\"}", "Invalid PESEL (too short)"},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"\"},\"description\":\"test\"}", "Empty PESEL"},
        });

        LOGGER.info("### POST /api/v2/permissions/entities/grants");
        probeAndLog("/api/v2/permissions/entities/grants", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Nip\",\"value\":\"123\"},\"description\":\"test\"}", "Invalid NIP format"},
        });

        LOGGER.info("### POST /api/v2/certificates/enrollments");
        probeAndLog("/api/v2/certificates/enrollments", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"certificateName\":null,\"certificateType\":null,\"csr\":null}", DESC_NULL_REQUIRED},
            {"{\"certificateName\":\"\",\"certificateType\":\"Authentication\",\"csr\":\"test\"}", "Empty certificate name"},
            {"{\"certificateName\":\"test<script>\",\"certificateType\":\"Authentication\",\"csr\":\"test\"}", "XSS in certificate name"},
            {"{\"certificateName\":\"test\",\"certificateType\":\"INVALID\",\"csr\":\"test\"}", "Invalid certificate type"},
        });

        LOGGER.info("### POST /api/v2/invoices/exports");
        probeAndLog("/api/v2/invoices/exports", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"encryption\":null,\"filters\":null}", DESC_NULL_REQUIRED},
            {"{\"encryption\":{},\"filters\":{}}", "Empty nested objects"},
        });

        LOGGER.info("### GET /api/v2/sessions/{ref}");
        probeEndpoint(METHOD_GET, "/api/v2/sessions/invalid-ref-number", null, true, DESC_INVALID_REF);
        probeEndpoint(METHOD_GET, "/api/v2/sessions/", null, true, "Empty reference number");
        probeEndpoint(METHOD_GET, "/api/v2/sessions/" + "A".repeat(TEST_LONG_REF_LENGTH), null, true, "Very long reference number");

        LOGGER.info("### GET /api/v2/auth/{ref}");
        probeEndpoint(METHOD_GET, "/api/v2/auth/invalid-ref", null, true, DESC_INVALID_REF);

        LOGGER.info("### GET /api/v2/tokens/{ref}");
        probeEndpoint(METHOD_GET, "/api/v2/tokens/invalid-ref", null, true, DESC_INVALID_REF);

        LOGGER.info("### DELETE /api/v2/tokens/{ref}");
        probeEndpoint(METHOD_DELETE, "/api/v2/tokens/invalid-ref-to-delete", null, true, DESC_INVALID_REF);

        LOGGER.info("### GET /api/v2/certificates/limits");
        probeEndpoint(METHOD_GET, "/api/v2/certificates/limits", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### GET /api/v2/certificates/enrollments/data");
        probeEndpoint(METHOD_GET, "/api/v2/certificates/enrollments/data", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### POST /api/v2/permissions/query/personal/grants");
        probeAndLog("/api/v2/permissions/query/personal/grants", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
            {"{\"pageSize\":0}", "pageSize=0"},
            {"{\"pageSize\":-1}", "pageSize=-1"},
            {"{\"pageSize\":999999}", "pageSize too large"},
        });

        LOGGER.info("### POST /api/v2/permissions/query/persons/grants");
        probeAndLog("/api/v2/permissions/query/persons/grants", true, new String[][]{
            {BODY_EMPTY_OBJECT, DESC_EMPTY_BODY},
        });

        LOGGER.info("### GET /api/v2/permissions/attachments/status");
        probeEndpoint(METHOD_GET, "/api/v2/permissions/attachments/status", null, true, DESC_VALID_REQUEST);

        LOGGER.info("### GET /api/v2/permissions/operations/{ref}");
        probeEndpoint(METHOD_GET, "/api/v2/permissions/operations/invalid-ref", null, true, DESC_INVALID_REF);

        LOGGER.info("### Wrong Content-Type test");
        probeWithContentType(PATH_INVOICES_QUERY_METADATA, "text/plain", "{\"subjectType\":\"Subject1\"}", true, "text/plain content type");
        probeWithContentType(PATH_INVOICES_QUERY_METADATA, "application/xml", "<xml/>", true, "XML content type");

        LOGGER.info("### Missing auth test");
        probeEndpoint(METHOD_GET, "/api/v2/limits/context", null, false, "No auth on authenticated endpoint");
        probeEndpoint(METHOD_POST, PATH_INVOICES_QUERY_METADATA, "{\"subjectType\":\"Subject1\"}", false, "No auth on POST endpoint");
    }

    private void probeAndLog(String path, boolean auth, String[][] tests) {
        LOGGER.info(LOG_TABLE_HEADER);
        LOGGER.info(LOG_TABLE_DIVIDER);
        for (String[] test : tests) {
            probeEndpoint(METHOD_POST, path, test[0], auth, test[1]);
        }
        LOGGER.info(EMPTY);
    }

    private void probeEndpoint(String method, String path, String body, boolean auth, String description) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(TIMEOUT);

            if (auth && bearerToken != null) {
                reqBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + bearerToken);
            }

            if (body != null) {
                reqBuilder.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            sendAndLog(reqBuilder.build(), description);

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOGGER.info(LOG_TABLE_ERROR_ROW, description, interrupted.getMessage());
        } catch (Exception failure) {
            LOGGER.info(LOG_TABLE_ERROR_ROW, description, failure.getMessage());
        }
    }

    private void probeWithContentType(String path, String contentType, String body, boolean auth, String description) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(TIMEOUT)
                    .header(HEADER_CONTENT_TYPE, contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (auth && bearerToken != null) {
                reqBuilder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + bearerToken);
            }

            sendAndLog(reqBuilder.build(), description);

        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            LOGGER.info(LOG_TABLE_ERROR_ROW, description, interrupted.getMessage());
        } catch (Exception failure) {
            LOGGER.info(LOG_TABLE_ERROR_ROW, description, failure.getMessage());
        }
    }

    private void sendAndLog(HttpRequest request, String description) throws java.io.IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (LOGGER.isInfoEnabled()) {
            String body = response.body();
            LOGGER.info(LOG_TABLE_ROW,
                    truncate(description),
                    response.statusCode(),
                    extractExceptionCode(body),
                    extractDetails(body));
        }
    }

    private static String extractExceptionCode(String json) {
        try {
            int prefixIndex = json.indexOf(EXCEPTION_CODE_FIELD_PREFIX);
            if (prefixIndex < 0) {
                return DASH;
            }
            String numericSlice = json.substring(prefixIndex + EXCEPTION_CODE_OFFSET,
                    Math.min(prefixIndex + EXCEPTION_CODE_MAX_OFFSET, json.length()));
            return numericSlice.replaceAll(NON_DIGIT_REGEX, EMPTY);
        } catch (Exception ignored) {
            return DASH;
        }
    }

    private static String extractDetails(String json) {
        try {
            int prefixIndex = json.indexOf(DETAILS_FIELD_PREFIX);
            if (prefixIndex < 0) {
                if (json.length() > DETAILS_MAX_LENGTH) {
                    return json.substring(0, DETAILS_MAX_LENGTH) + ELLIPSIS;
                }
                return shortSummary(json);
            }
            int closingBracketIndex = json.indexOf("]", prefixIndex);
            if (closingBracketIndex < 0) {
                return json.substring(prefixIndex, Math.min(prefixIndex + DETAILS_MAX_LENGTH, json.length()));
            }
            String details = json.substring(prefixIndex + DETAILS_FIELD_PREFIX.length(), closingBracketIndex);
            details = decodePolishEscapes(details);
            if (details.length() > DETAILS_MAX_LENGTH) {
                details = details.substring(0, DETAILS_MAX_LENGTH) + ELLIPSIS;
            }
            return details;
        } catch (Exception ignored) {
            return shortSummary(json);
        }
    }

    private static String decodePolishEscapes(String value) {
        return value.replace("\\u0027", "'")
                .replace("\\u0142", "l")
                .replace("\\u0105", "a")
                .replace("\\u015B", "s")
                .replace("\\u0119", "e");
    }

    private static String truncate(String value) {
        return value.length() > DESCRIPTION_MAX_LENGTH
                ? value.substring(0, DESCRIPTION_MAX_LENGTH - TRUNCATE_ELLIPSIS_RESERVE) + ELLIPSIS
                : value;
    }

    private static String shortSummary(String json) {
        return json.length() > SHORT_SUMMARY_MAX_LENGTH
                ? json.substring(0, SHORT_SUMMARY_MAX_LENGTH) + ELLIPSIS
                : json;
    }
}
