/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.config;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Authentication-time IP allow-list policy. Codex 2026-05-05 #7 / F6.
 *
 * <p>The KSeF API ({@code AllowedIps} schema in {@code openapi/open-api.json})
 * accepts up to 10 entries per kind:
 * <ul>
 *   <li>exact IPv4 addresses ({@link #ip4Addresses()})</li>
 *   <li>IPv4 ranges of the form {@code start-end}, e.g. {@code 10.0.0.1-10.0.0.254}
 *       ({@link #ip4Ranges()})</li>
 *   <li>IPv4 CIDR masks of the form {@code addr/prefix}, e.g.
 *       {@code 192.168.0.0/24} ({@link #ip4Masks()})</li>
 * </ul>
 *
 * <p>By default the SDK passes only the challenge's reported
 * {@code clientIp} as a single exact address — sufficient for direct
 * client-server use. Consumers that authenticate from a NAT'd or
 * load-balanced environment, or that need to share a token across a
 * known address pool, supply a custom policy via
 * {@link KsefCredentials#authorizationPolicy()} (set per-credentials).
 *
 * <p>Validation: each list entry is checked against the OpenAPI regex
 * pattern at construction time; malformed entries throw
 * {@link IllegalArgumentException} immediately rather than reaching
 * the server.
 *
 * @since 1.0.0
 */
public record AuthorizationPolicy(
        List<String> ip4Addresses,
        List<String> ip4Ranges,
        List<String> ip4Masks) {

    private static final int MAX_PER_KIND = 10;

    /** Each octet 0-255 with mandatory dots between them — tightened beyond the OpenAPI spec regex which permitted optional dots. */
    private static final String IPV4_OCTET = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)";
    private static final String IPV4_FULL = IPV4_OCTET + "\\." + IPV4_OCTET + "\\." + IPV4_OCTET + "\\." + IPV4_OCTET;
    private static final Pattern IPV4_ADDRESS = Pattern.compile("^" + IPV4_FULL + "$");
    private static final Pattern IPV4_RANGE = Pattern.compile("^" + IPV4_FULL + "-" + IPV4_FULL + "$");
    private static final Pattern IPV4_MASK = Pattern.compile(
            "^" + IPV4_FULL + "/(0|[1-9]|1\\d|2\\d|3[0-2])$");

    private static final String ERR_TOO_MANY = "%s exceeds spec limit of " + MAX_PER_KIND + " (got %d)";
    private static final String ERR_ADDR_INVALID = "ip4Addresses[%d] is not a valid IPv4 address: %s";
    private static final String ERR_RANGE_INVALID = "ip4Ranges[%d] is not a valid IPv4 range (start-end): %s";
    private static final String ERR_MASK_INVALID = "ip4Masks[%d] is not a valid IPv4 CIDR (addr/prefix): %s";

    public AuthorizationPolicy {
        Objects.requireNonNull(ip4Addresses, "ip4Addresses must not be null (use empty list for none)");
        Objects.requireNonNull(ip4Ranges, "ip4Ranges must not be null (use empty list for none)");
        Objects.requireNonNull(ip4Masks, "ip4Masks must not be null (use empty list for none)");
        validateSize("ip4Addresses", ip4Addresses);
        validateSize("ip4Ranges", ip4Ranges);
        validateSize("ip4Masks", ip4Masks);
        for (int idx = 0; idx < ip4Addresses.size(); idx++) {
            if (!IPV4_ADDRESS.matcher(ip4Addresses.get(idx)).matches()) {
                throw new IllegalArgumentException(String.format(ERR_ADDR_INVALID, idx, ip4Addresses.get(idx)));
            }
        }
        for (int idx = 0; idx < ip4Ranges.size(); idx++) {
            if (!IPV4_RANGE.matcher(ip4Ranges.get(idx)).matches()) {
                throw new IllegalArgumentException(String.format(ERR_RANGE_INVALID, idx, ip4Ranges.get(idx)));
            }
        }
        for (int idx = 0; idx < ip4Masks.size(); idx++) {
            if (!IPV4_MASK.matcher(ip4Masks.get(idx)).matches()) {
                throw new IllegalArgumentException(String.format(ERR_MASK_INVALID, idx, ip4Masks.get(idx)));
            }
        }
        ip4Addresses = List.copyOf(ip4Addresses);
        ip4Ranges = List.copyOf(ip4Ranges);
        ip4Masks = List.copyOf(ip4Masks);
    }

    private static void validateSize(String field, List<?> values) {
        if (values.size() > MAX_PER_KIND) {
            throw new IllegalArgumentException(String.format(ERR_TOO_MANY, field, values.size()));
        }
    }

    /** Convenience: a policy permitting only the supplied IPv4 address. */
    public static AuthorizationPolicy onlyAddress(String ipv4) {
        return new AuthorizationPolicy(List.of(ipv4), List.of(), List.of());
    }

    /** Convenience: a policy permitting only the supplied CIDR. */
    public static AuthorizationPolicy onlyCidr(String cidr) {
        return new AuthorizationPolicy(List.of(), List.of(), List.of(cidr));
    }
}
