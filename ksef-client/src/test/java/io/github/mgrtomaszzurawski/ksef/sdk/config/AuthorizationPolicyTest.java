/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Codex 2026-05-05 #7 / F6 — public AuthorizationPolicy validation
 * mirrors the OpenAPI {@code AllowedIps} schema regex constraints.
 */
class AuthorizationPolicyTest {

    @Test
    void onlyAddress_factoryProducesSingleAddressPolicy() {
        AuthorizationPolicy policy = AuthorizationPolicy.onlyAddress("192.168.1.10");
        assertEquals(List.of("192.168.1.10"), policy.ip4Addresses());
        assertEquals(List.of(), policy.ip4Ranges());
        assertEquals(List.of(), policy.ip4Masks());
    }

    @Test
    void onlyCidr_factoryProducesSingleMaskPolicy() {
        AuthorizationPolicy policy = AuthorizationPolicy.onlyCidr("10.0.0.0/24");
        assertEquals(List.of("10.0.0.0/24"), policy.ip4Masks());
    }

    @Test
    void canonicalConstructor_acceptsAllThreeKinds() {
        AuthorizationPolicy policy = new AuthorizationPolicy(
                List.of("192.168.0.10", "10.0.0.5"),
                List.of("172.16.0.1-172.16.0.255"),
                List.of("192.168.0.0/24", "10.0.0.0/8"));
        assertEquals(2, policy.ip4Addresses().size());
        assertEquals(1, policy.ip4Ranges().size());
        assertEquals(2, policy.ip4Masks().size());
    }

    @Test
    void canonicalConstructor_rejectsInvalidAddress() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(List.of("not-an-ip"), List.of(), List.of()));
        assertEquals(true, ex.getMessage().contains("not a valid IPv4 address"));
    }

    @Test
    void canonicalConstructor_rejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(List.of(), List.of("10.0.0.1-bad"), List.of()));
    }

    @Test
    void canonicalConstructor_rejectsInvalidCidr() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(List.of(), List.of(), List.of("10.0.0.0/33")));
    }

    @Test
    void canonicalConstructor_rejectsTooManyEntries() {
        List<String> eleven = List.of("1.1.1.1", "1.1.1.2", "1.1.1.3", "1.1.1.4", "1.1.1.5",
                "1.1.1.6", "1.1.1.7", "1.1.1.8", "1.1.1.9", "1.1.1.10", "1.1.1.11");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationPolicy(eleven, List.of(), List.of()));
        assertEquals(true, ex.getMessage().contains("exceeds spec limit"));
    }
}
