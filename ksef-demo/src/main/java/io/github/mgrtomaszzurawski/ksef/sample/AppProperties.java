/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads demo application properties from a file with environment variable substitution.
 * Supports {@code ${ENV_VAR:default}} placeholder syntax.
 */
public final class AppProperties {

    private static final String KEY_TOKEN = "ksef.token";
    private static final String KEY_NIP = "ksef.nip";
    private static final String KEY_ENVIRONMENT = "ksef.environment";
    private static final String KEY_CERT_FILE = "ksef.cert.file";
    private static final String KEY_CERT_PASSWORD = "ksef.cert.password";
    private static final String KEY_TEST_ENVIRONMENT = "ksef.test.environment";
    private static final String ERR_LOAD = "Failed to load properties from: ";

    private final String ksefToken;
    private final String nipIdentifier;
    private final String environment;
    private final String certFile;
    private final String certPassword;
    private final String testEnvironment;

    private AppProperties(String ksefToken, String nipIdentifier, String environment,
                          String certFile, String certPassword, String testEnvironment) {
        this.ksefToken = ksefToken;
        this.nipIdentifier = nipIdentifier;
        this.environment = environment;
        this.certFile = certFile;
        this.certPassword = certPassword;
        this.testEnvironment = testEnvironment;
    }

    public String ksefToken() { return ksefToken; }
    public String nipIdentifier() { return nipIdentifier; }
    public String environment() { return environment; }
    public String certFile() { return certFile; }
    public String certPassword() { return certPassword; }

    /**
     * Optional KSeF TEST environment URL (e.g. {@code https://api-test.ksef.mf.gov.pl}).
     * When set, the demo runs an additional pass against TEST env using auto-generated
     * NIP / peppolId / vat-UE id authenticated with on-the-fly self-signed certificates.
     * That pass exercises form-code/auth-context combinations DEMO env cannot simulate
     * (FA(2), Peppol provider, EU VAT UE).
     *
     * @return the TEST env URL, or {@code null} when not configured
     */
    public String testEnvironment() { return testEnvironment; }

    public boolean hasCertificate() {
        return certFile != null && !certFile.isBlank();
    }

    public boolean hasTestEnvironment() {
        return testEnvironment != null && !testEnvironment.isBlank();
    }

    /**
     * Load properties from a file path. Resolves environment variable placeholders.
     */
    public static AppProperties load(Path file) {
        Properties props = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            props.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException(ERR_LOAD + file, exception);
        }
        return new AppProperties(
                resolve(props.getProperty(KEY_TOKEN)),
                resolve(props.getProperty(KEY_NIP)),
                resolve(props.getProperty(KEY_ENVIRONMENT)),
                resolve(props.getProperty(KEY_CERT_FILE)),
                resolve(props.getProperty(KEY_CERT_PASSWORD)),
                resolve(props.getProperty(KEY_TEST_ENVIRONMENT))
        );
    }

    /**
     * Resolve {@code ${ENV_VAR:default}} placeholders in a property value.
     */
    static String resolve(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return trimmed;
        }
        String inner = trimmed.substring(2, trimmed.length() - 1);
        int colonIndex = inner.indexOf(':');
        if (colonIndex < 0) {
            String envValue = System.getenv(inner);
            return envValue != null ? envValue : "";
        }
        String envName = inner.substring(0, colonIndex);
        String defaultValue = inner.substring(colonIndex + 1);
        String envValue = System.getenv(envName);
        return (envValue != null && !envValue.isBlank()) ? envValue : defaultValue;
    }
}
