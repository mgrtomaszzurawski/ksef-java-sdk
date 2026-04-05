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
/*
 * KSeF Sample App - Validation probe for Phase 7.0
 * Tests server-side validation by sending invalid inputs to all endpoints.
 */
package io.github.mgrtomaszzurawski.ksef.sample;


import io.github.mgrtomaszzurawski.ksef.sdk.KsefClient;
import io.github.mgrtomaszzurawski.ksef.sdk.KsefEnvironment;
import io.github.mgrtomaszzurawski.ksef.sdk.exception.KsefException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Properties;

/**
 * Validation probe — sends invalid requests to KSeF endpoints and logs
 * server error responses. Output goes to stdout as markdown-formatted
 * validation results.
 */
public final class ValidationProbe {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationProbe.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

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
        try (FileInputStream fis = new FileInputStream("ksef-credentials.properties")) {
            props.load(fis);
        }

        String ksefUrl = props.getProperty("ksef.environment");
        String ksefToken = props.getProperty("ksef.token");
        String nipIdentifier = props.getProperty("ksef.nip");

        LOG.info("=== KSeF Validation Probe ===");
        LOG.info("Environment: {}", ksefUrl);
        LOG.info("NIP: {}", nipIdentifier);

        // Authenticate to get bearer token
        KsefClient client = KsefClient.builder(KsefEnvironment.custom(ksefUrl))
                .build();

        PublicKey publicKey = client.security().getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().stream()
                        .anyMatch(u -> u == io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION))
                .findFirst()
                .map(cert -> io.github.mgrtomaszzurawski.ksef.sdk.crypto.CryptoService.parsePublicKeyFromPem(
                        new String(java.util.Base64.getEncoder().encode(cert.certificate()))))
                .orElseThrow(() -> new RuntimeException("No token encryption certificate found"));

        // Actually need to parse the X509 cert properly
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificate tokenCert = client.security().getPublicKeyCertificates().stream()
                .filter(cert -> cert.usage().stream()
                        .anyMatch(u -> u == io.github.mgrtomaszzurawski.ksef.sdk.model.PublicKeyCertificateUsage.KSEF_TOKEN_ENCRYPTION))
                .findFirst()
                .orElseThrow();
        java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(tokenCert.certificate()));
        publicKey = x509.getPublicKey();

        var challenge = client.auth().requestChallenge();
        var authInit = client.auth().authenticateWithToken(challenge, ksefToken, nipIdentifier, publicKey);
        LOG.info("Auth ref: {}", authInit.referenceNumber());

        // Poll for auth status
        int authStatus = 0;
        for (int attempt = 0; attempt < 10; attempt++) {
            Thread.sleep(2000);
            var status = client.auth().getStatus(authInit.referenceNumber());
            authStatus = status.status().code();
            LOG.info("Auth status: {}", authStatus);
            if (authStatus == 200) break;
        }

        if (authStatus != 200) {
            LOG.error("Auth failed with status {}", authStatus);
            return;
        }

        var tokens = client.auth().redeemTokens();
        String bearer = tokens.accessToken().token();
        LOG.info("Got bearer token, length={}", bearer.length());

        // Now probe all endpoints
        ValidationProbe probe = new ValidationProbe(ksefUrl, bearer);
        probe.runAllProbes();

        // Clean up - terminate session
        client.auth().terminateCurrentSession();
        LOG.info("Session terminated. Probe complete.");
    }

    private void runAllProbes() {
        System.out.println("# KSeF Server-Side Validation Probe Results\n");
        System.out.println("Generated: " + java.time.OffsetDateTime.now() + "\n");

        probeUnauthEndpoints();
        probeAuthEndpoints();
    }

    private void probeUnauthEndpoints() {
        System.out.println("## Unauthenticated Endpoints\n");

        // GET /security/public-key-certificates — no body, always 200
        probeEndpoint("GET", "/api/v2/security/public-key-certificates", null, false,
                "No body needed, always returns certificates");

        // POST /auth/challenge — no body needed
        probeEndpoint("POST", "/api/v2/auth/challenge", null, false,
                "No body needed");
        probeEndpoint("POST", "/api/v2/auth/challenge", "{}", false,
                "Empty body accepted");
        probeEndpoint("POST", "/api/v2/auth/challenge", "{\"foo\":\"bar\"}", false,
                "Unknown fields ignored");

        // POST /auth/ksef-token — requires body
        System.out.println("### POST /api/v2/auth/ksef-token\n");
        probeAndLog("/api/v2/auth/ksef-token", false, new String[][]{
            {"{}", "Empty body"},
            {"{\"challenge\":null,\"contextIdentifier\":null,\"encryptedToken\":null}", "Null required fields"},
            {"{\"challenge\":\"\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"\"},\"encryptedToken\":\"\"}", "Empty strings"},
            {"{\"challenge\":123}", "Wrong type (int for string)"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"INVALID\",\"value\":\"1234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "Invalid enum value"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"123\"},\"encryptedToken\":\"dGVzdA==\"}", "Invalid NIP (too short)"},
            {"{\"challenge\":\"test\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"12345678901234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "NIP too long"},
            {"{\"challenge\":\"" + "A".repeat(500) + "\",\"contextIdentifier\":{\"type\":\"Nip\",\"value\":\"1234567890\"},\"encryptedToken\":\"dGVzdA==\"}", "Very long challenge (500 chars)"},
        });
    }

    private void probeAuthEndpoints() {
        System.out.println("## Authenticated Endpoints\n");

        // POST /invoices/query/metadata
        System.out.println("### POST /api/v2/invoices/query/metadata\n");
        probeAndLog("/api/v2/invoices/query/metadata", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"subjectType\":null}", "Null subjectType"},
            {"{\"subjectType\":\"INVALID\"}", "Invalid enum"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":null}", "Null dateRange"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":null,\"from\":null}}", "Null nested fields"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"not-a-date\"}}", "Invalid date format"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":0}", "pageSize=0"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":-1}", "pageSize=-1"},
            {"{\"subjectType\":\"Subject1\",\"dateRange\":{\"dateType\":\"Invoicing\",\"from\":\"2026-01-01T00:00:00+00:00\"},\"pageSize\":999999}", "pageSize=999999"},
        });

        // POST /tokens (generate)
        System.out.println("### POST /api/v2/tokens\n");
        probeAndLog("/api/v2/tokens", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"description\":null}", "Null description"},
            {"{\"description\":\"\"}", "Empty description"},
            {"{\"description\":\"test\",\"requestedPermissions\":null}", "Null permissions"},
            {"{\"description\":\"test\",\"requestedPermissions\":[]}", "Empty permissions array"},
            {"{\"description\":\"test\",\"requestedPermissions\":[\"INVALID\"]}", "Invalid permission type"},
            {"{\"description\":\"" + "A".repeat(300) + "\",\"requestedPermissions\":[\"InvoiceRead\"]}", "Very long description (300 chars)"},
        });

        // GET /limits/context
        System.out.println("### GET /api/v2/limits/context\n");
        probeEndpoint("GET", "/api/v2/limits/context", null, true, "Valid request");

        // GET /limits/subject
        System.out.println("### GET /api/v2/limits/subject\n");
        probeEndpoint("GET", "/api/v2/limits/subject", null, true, "Valid request");

        // GET /rate-limits
        System.out.println("### GET /api/v2/rate-limits\n");
        probeEndpoint("GET", "/api/v2/rate-limits", null, true, "Valid request");

        // POST /permissions/persons/grants
        System.out.println("### POST /api/v2/permissions/persons/grants\n");
        probeAndLog("/api/v2/permissions/persons/grants", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"permission\":null,\"subjectIdentifier\":null}", "Null required fields"},
            {"{\"permission\":\"INVALID\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"12345678901\"},\"description\":\"test\"}", "Invalid permission enum"},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"123\"},\"description\":\"test\"}", "Invalid PESEL (too short)"},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Pesel\",\"value\":\"\"},\"description\":\"test\"}", "Empty PESEL"},
        });

        // POST /permissions/entities/grants
        System.out.println("### POST /api/v2/permissions/entities/grants\n");
        probeAndLog("/api/v2/permissions/entities/grants", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"permission\":\"InvoiceRead\",\"subjectIdentifier\":{\"type\":\"Nip\",\"value\":\"123\"},\"description\":\"test\"}", "Invalid NIP format"},
        });

        // POST /certificates/enrollments
        System.out.println("### POST /api/v2/certificates/enrollments\n");
        probeAndLog("/api/v2/certificates/enrollments", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"certificateName\":null,\"certificateType\":null,\"csr\":null}", "Null required fields"},
            {"{\"certificateName\":\"\",\"certificateType\":\"Authentication\",\"csr\":\"test\"}", "Empty certificate name"},
            {"{\"certificateName\":\"test<script>\",\"certificateType\":\"Authentication\",\"csr\":\"test\"}", "XSS in certificate name"},
            {"{\"certificateName\":\"test\",\"certificateType\":\"INVALID\",\"csr\":\"test\"}", "Invalid certificate type"},
        });

        // POST /invoices/exports
        System.out.println("### POST /api/v2/invoices/exports\n");
        probeAndLog("/api/v2/invoices/exports", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"encryption\":null,\"filters\":null}", "Null required fields"},
            {"{\"encryption\":{},\"filters\":{}}", "Empty nested objects"},
        });

        // GET /sessions/{ref} with invalid ref
        System.out.println("### GET /api/v2/sessions/{ref}\n");
        probeEndpoint("GET", "/api/v2/sessions/invalid-ref-number", null, true, "Invalid reference number");
        probeEndpoint("GET", "/api/v2/sessions/", null, true, "Empty reference number");
        probeEndpoint("GET", "/api/v2/sessions/" + "A".repeat(200), null, true, "Very long reference number");

        // GET /auth/{ref} with invalid ref
        System.out.println("### GET /api/v2/auth/{ref}\n");
        probeEndpoint("GET", "/api/v2/auth/invalid-ref", null, true, "Invalid reference number");

        // GET /tokens/{ref} with invalid ref
        System.out.println("### GET /api/v2/tokens/{ref}\n");
        probeEndpoint("GET", "/api/v2/tokens/invalid-ref", null, true, "Invalid reference number");

        // DELETE /tokens/{ref} with invalid ref
        System.out.println("### DELETE /api/v2/tokens/{ref}\n");
        probeEndpoint("DELETE", "/api/v2/tokens/invalid-ref-to-delete", null, true, "Invalid reference number");

        // GET /certificates/limits
        System.out.println("### GET /api/v2/certificates/limits\n");
        probeEndpoint("GET", "/api/v2/certificates/limits", null, true, "Valid request");

        // GET /certificates/enrollments/data
        System.out.println("### GET /api/v2/certificates/enrollments/data\n");
        probeEndpoint("GET", "/api/v2/certificates/enrollments/data", null, true, "Valid request");

        // POST /permissions/query/personal/grants
        System.out.println("### POST /api/v2/permissions/query/personal/grants\n");
        probeAndLog("/api/v2/permissions/query/personal/grants", true, new String[][]{
            {"{}", "Empty body"},
            {"{\"pageSize\":0}", "pageSize=0"},
            {"{\"pageSize\":-1}", "pageSize=-1"},
            {"{\"pageSize\":999999}", "pageSize too large"},
        });

        // POST /permissions/query/persons/grants
        System.out.println("### POST /api/v2/permissions/query/persons/grants\n");
        probeAndLog("/api/v2/permissions/query/persons/grants", true, new String[][]{
            {"{}", "Empty body"},
        });

        // GET /permissions/attachments/status
        System.out.println("### GET /api/v2/permissions/attachments/status\n");
        probeEndpoint("GET", "/api/v2/permissions/attachments/status", null, true, "Valid request");

        // GET /permissions/operations/{ref}
        System.out.println("### GET /api/v2/permissions/operations/{ref}\n");
        probeEndpoint("GET", "/api/v2/permissions/operations/invalid-ref", null, true, "Invalid reference number");

        // Test with completely wrong Content-Type
        System.out.println("### Wrong Content-Type test\n");
        probeWithContentType("/api/v2/invoices/query/metadata", "text/plain", "{\"subjectType\":\"Subject1\"}", true, "text/plain content type");
        probeWithContentType("/api/v2/invoices/query/metadata", "application/xml", "<xml/>", true, "XML content type");

        // Test without auth on authenticated endpoint
        System.out.println("### Missing auth test\n");
        probeEndpoint("GET", "/api/v2/limits/context", null, false, "No auth on authenticated endpoint");
        probeEndpoint("POST", "/api/v2/invoices/query/metadata", "{\"subjectType\":\"Subject1\"}", false, "No auth on POST endpoint");
    }

    private void probeAndLog(String path, boolean auth, String[][] tests) {
        System.out.println("| Input | HTTP | Code | Details |");
        System.out.println("|-------|------|------|---------|");
        for (String[] test : tests) {
            probeEndpoint("POST", path, test[0], auth, test[1]);
        }
        System.out.println();
    }

    private void probeEndpoint(String method, String path, String body, boolean auth, String description) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(TIMEOUT);

            if (auth && bearerToken != null) {
                reqBuilder.header("Authorization", "Bearer " + bearerToken);
            }

            if (body != null) {
                reqBuilder.header("Content-Type", "application/json");
                reqBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else {
                reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            String respBody = resp.body().length() > 500
                    ? resp.body().substring(0, 500) + "..."
                    : resp.body();

            System.out.printf("| %s | %d | %s | %s |%n",
                    truncate(description, 40),
                    resp.statusCode(),
                    extractExceptionCode(resp.body()),
                    extractDetails(resp.body()));

        } catch (Exception ex) {
            System.out.printf("| %s | ERR | - | %s |%n", description, ex.getMessage());
        }
    }

    private void probeWithContentType(String path, String contentType, String body, boolean auth, String description) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(TIMEOUT)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (auth && bearerToken != null) {
                reqBuilder.header("Authorization", "Bearer " + bearerToken);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            System.out.printf("| %s | %d | %s | %s |%n",
                    truncate(description, 40),
                    resp.statusCode(),
                    extractExceptionCode(resp.body()),
                    extractDetails(resp.body()));

        } catch (Exception ex) {
            System.out.printf("| %s | ERR | - | %s |%n", description, ex.getMessage());
        }
    }

    private static String extractExceptionCode(String json) {
        try {
            int idx = json.indexOf("\"exceptionCode\":");
            if (idx < 0) return "-";
            String sub = json.substring(idx + 16, Math.min(idx + 22, json.length()));
            return sub.replaceAll("[^0-9]", "");
        } catch (Exception e) {
            return "-";
        }
    }

    private static String extractDetails(String json) {
        try {
            int idx = json.indexOf("\"details\":[");
            if (idx < 0) {
                // Try to get a short summary
                if (json.length() > 200) return json.substring(0, 200) + "...";
                return json.length() > 100 ? json.substring(0, 100) + "..." : json;
            }
            int end = json.indexOf("]", idx);
            if (end < 0) return json.substring(idx, Math.min(idx + 200, json.length()));
            String details = json.substring(idx + 11, end);
            // Unescape unicode
            details = details.replace("\\u0027", "'")
                    .replace("\\u0142", "l")
                    .replace("\\u0105", "a")
                    .replace("\\u015B", "s")
                    .replace("\\u0119", "e");
            if (details.length() > 200) details = details.substring(0, 200) + "...";
            return details;
        } catch (Exception e) {
            return json.length() > 100 ? json.substring(0, 100) + "..." : json;
        }
    }

    private static String truncate(String str, int maxLen) {
        return str.length() > maxLen ? str.substring(0, maxLen - 3) + "..." : str;
    }
}
