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
package io.github.mgrtomaszzurawski.ksef.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent state from demo runs. Saved to demo-state.json for cross-run tracking
 * and cleanup of orphaned resources.
 */
public final class DemoState {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private String lastRunTimestamp;
    private String lastRunMode;
    private String invoiceKsefNumber;
    private String sessionReferenceNumber;
    private String exportReferenceNumber;
    private List<String> createdTokenRefs = new ArrayList<>();
    private List<String> createdPermissionIds = new ArrayList<>();
    private List<String> enrolledCertificateSerials = new ArrayList<>();

    public String getLastRunTimestamp() { return lastRunTimestamp; }
    public void setLastRunTimestamp(String timestamp) { this.lastRunTimestamp = timestamp; }

    public String getLastRunMode() { return lastRunMode; }
    public void setLastRunMode(String mode) { this.lastRunMode = mode; }

    public String getInvoiceKsefNumber() { return invoiceKsefNumber; }
    public void setInvoiceKsefNumber(String number) { this.invoiceKsefNumber = number; }

    public String getSessionReferenceNumber() { return sessionReferenceNumber; }
    public void setSessionReferenceNumber(String ref) { this.sessionReferenceNumber = ref; }

    public String getExportReferenceNumber() { return exportReferenceNumber; }
    public void setExportReferenceNumber(String ref) { this.exportReferenceNumber = ref; }

    public List<String> getCreatedTokenRefs() { return createdTokenRefs; }
    public void setCreatedTokenRefs(List<String> refs) { this.createdTokenRefs = refs; }

    public List<String> getCreatedPermissionIds() { return createdPermissionIds; }
    public void setCreatedPermissionIds(List<String> ids) { this.createdPermissionIds = ids; }

    public List<String> getEnrolledCertificateSerials() { return enrolledCertificateSerials; }
    public void setEnrolledCertificateSerials(List<String> serials) { this.enrolledCertificateSerials = serials; }

    /**
     * Load state from JSON file, or return empty state if file does not exist.
     */
    public static DemoState load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new DemoState();
        }
        return MAPPER.readValue(file.toFile(), DemoState.class);
    }

    /**
     * Save state to JSON file.
     */
    public void save(Path file) throws IOException {
        MAPPER.writeValue(file.toFile(), this);
    }
}
