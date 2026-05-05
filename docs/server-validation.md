# KSeF Server-Side Validation — Summary

**Date:** 2026-04-05
**Environment:** api-demo.ksef.mf.gov.pl
**Auth method:** KSeF token (NIP 8992713806)

## Error Response Mechanism

KSeF server (.NET backend) returns structured validation errors:

```json
{
  "exception": {
    "exceptionDetailList": [{
      "exceptionCode": 21405,
      "exceptionDescription": "Błąd walidacji danych wejściowych.",
      "details": [
        "'challenge' must not be empty.",
        "'contextIdentifier.value' must not be empty.",
        "Invalid NIP format."
      ]
    }],
    "serviceCode": "trace-id",
    "timestamp": "2026-04-05T18:48:35Z"
  }
}
```

## Exception Codes

| Code | Meaning | HTTP Status |
|------|---------|-------------|
| 21405 | Input validation error — details list per-field errors with dot-notation paths | 400 |
| 21001 | JSON parsing error — wrong types, reveals .NET internal type paths | 400 |
| 25001 | Business rule error (e.g., cert enrollment not available for token auth) | 400 |
| - | WAF block — `<script>` tags trigger external security service | 403 |
| - | Missing/invalid auth | 401 |
| - | Wrong Content-Type | 415 |
| - | Rate limit exceeded — Polish message with retry-after | 429 |

## Key Validation Behaviors

### Field-level validation
- Server validates ALL required fields in one pass, returns ALL errors at once
- Uses dot-notation for nested paths: `filters.dateRange.from`, `contextIdentifier.value`
- Validates format of: NIP, PESEL, reference numbers, challenge strings, dates
- Unknown fields are **silently ignored** (no strict schema validation)
- `null` and `""` (empty string) treated the same for most required fields

### Type validation
- Wrong JSON types (int where string expected) → exceptionCode 21001
- Invalid enum values → exceptionCode 21001 with .NET type path (`Ksef.Api.Core.Tokens.Models.AuthenticationContextIdentifierType`)
- The .NET type paths reveal internal server model names

### Business constraints discovered
- `description`: min 5, max 256 characters (permissions grants)
- `dateRange`: max 3 months span
- `pageSize`: NOT validated by permission queries (0, -1, 999999 all accepted → returns empty/full results)
- NIP: format-validated (10 digits, checksum)
- PESEL: format-validated (11 digits, checksum)
- Reference numbers: format-validated (pattern: `YYYYMMDD-XX-...`)
- Certificate name: pattern `^[a-zA-Z0-9_\- ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]+$` (spec says so, WAF blocks `<script>`)

### Auth behavior
- Unauthenticated endpoints: GET /security/public-key-certificates, POST /auth/challenge
- POST /auth/challenge accepts ANY body (or no body), always returns 200
- All other endpoints require Bearer token → 401 without it

### Content-Type
- Only `application/json` accepted for POST bodies → 415 for anything else
- GET requests don't need Content-Type

## Implications for SDK Builders (Phase 7.2)

### Must validate client-side
1. **Required fields** — server validates but SDK should fail-fast before HTTP call
2. **NIP format** — 10 digits + checksum (server validates but poor error message for consumer)
3. **PESEL format** — 11 digits + checksum
4. **Date ranges** — max 3 months
5. **Description length** — min 5, max 256 chars

### Can rely on server-side
1. **Reference number format** — server validates consistently, no need to replicate pattern
2. **Enum values** — server rejects invalid enums with clear error
3. **Challenge format** — server validates, SDK doesn't need to
4. **Nested field structure** — server validates deep paths

### Don't need to validate
1. **Unknown fields** — server ignores them
2. **pageSize** — server accepts any value for permission queries
3. **Content-Type** — SDK always sends application/json
