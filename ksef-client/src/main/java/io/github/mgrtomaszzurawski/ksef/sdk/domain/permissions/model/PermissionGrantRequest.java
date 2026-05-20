/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.permissions.model;

/**
 * Sealed parent of all permission-grant request types accepted by
 * {@code Permissions.grant(...)}. Each concrete implementation maps
 * 1-to-1 to a separate KSeF wire endpoint with distinct required
 * fields per spec ({@code ksef-docs/uprawnienia.md}):
 *
 * <ul>
 *   <li>{@link PersonPermissionGrantRequest} — grant work permissions
 *       to a person (PESEL/NIP/fingerprint subject) in the current
 *       context. Wire: {@code POST /permissions/persons/grants}.</li>
 *   <li>{@link EntityPermissionGrantRequest} — grant invoice-handling
 *       permissions to another entity by NIP. Wire:
 *       {@code POST /permissions/entities/grants}.</li>
 *   <li>{@link EntityAuthorizationPermissionGrantRequest} — grant
 *       authorisation permissions (SelfInvoicing / RRInvoicing /
 *       TaxRepresentative / PefInvoicing) to an entity. Wire:
 *       {@code POST /permissions/authorizations/grants}.</li>
 *   <li>{@link IndirectPermissionGrantRequest} — grant permissions
 *       indirectly through an intermediary for a specific target
 *       entity. Wire: {@code POST /permissions/indirect/grants}.</li>
 *   <li>{@link SubunitPermissionGrantRequest} — grant administrator
 *       permissions to a subordinate unit (InternalId-typed
 *       subject). Wire:
 *       {@code POST /permissions/subunits/grants}.</li>
 *   <li>{@link EuEntityAdminPermissionGrantRequest} — grant EU entity
 *       administrator permissions (VAT UE context). Wire:
 *       {@code POST /permissions/eu-entity-admin/grants}.</li>
 *   <li>{@link EuEntityPermissionGrantRequest} — grant EU entity
 *       permissions (VAT UE context). Wire:
 *       {@code POST /permissions/eu-entity/grants}.</li>
 * </ul>
 *
 * <p>R2-11a: collapses the previous 7-named-methods pattern
 * ({@code grantPerson}, {@code grantEntity}, …) into a single
 * {@code grant(PermissionGrantRequest)} entry point with sealed
 * dispatch in the implementation. The wire endpoints stay separate
 * per spec; only the consumer-facing surface is unified. Pattern-
 * matching on the concrete subtype gives exhaustiveness checking in
 * the SDK impl and IDE-discoverable builder navigation for consumers
 * (auto-complete on {@code PermissionGrantRequest} surfaces all 7
 * concrete builders).
 *
 * <p>The 7-type set is fixed by KSeF spec; new permission categories
 * land via SDK minor-version bumps that extend the {@code permits}
 * clause.
 *
 * @since 0.1.0
 */
public sealed interface PermissionGrantRequest
        permits PersonPermissionGrantRequest,
                EntityPermissionGrantRequest,
                EntityAuthorizationPermissionGrantRequest,
                IndirectPermissionGrantRequest,
                SubunitPermissionGrantRequest,
                EuEntityAdminPermissionGrantRequest,
                EuEntityPermissionGrantRequest {
}
