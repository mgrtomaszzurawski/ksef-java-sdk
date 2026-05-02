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
    private static final String ERR_LOAD = "Failed to load properties from: ";

    private final String ksefToken;
    private final String nipIdentifier;
    private final String environment;
    private final String certFile;
    private final String certPassword;

    private AppProperties(String ksefToken, String nipIdentifier, String environment,
                          String certFile, String certPassword) {
        this.ksefToken = ksefToken;
        this.nipIdentifier = nipIdentifier;
        this.environment = environment;
        this.certFile = certFile;
        this.certPassword = certPassword;
    }

    public String ksefToken() { return ksefToken; }
    public String nipIdentifier() { return nipIdentifier; }
    public String environment() { return environment; }
    public String certFile() { return certFile; }
    public String certPassword() { return certPassword; }

    public boolean hasCertificate() {
        return certFile != null && !certFile.isBlank();
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
                resolve(props.getProperty(KEY_CERT_PASSWORD))
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
