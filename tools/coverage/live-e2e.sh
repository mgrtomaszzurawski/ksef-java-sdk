#!/usr/bin/env bash
# Copyright (c) 2026 Tomasz Zurawski
# SPDX-License-Identifier: AGPL-3.0-only
#
# live-e2e.sh -- fail-closed live write->read against the KSeF demo server.
#
# Green WireMock is not merge proof: a mock asserts the AUTHOR'S guess of the
# wire shape. This drives a real round trip through the SDK against
# api-demo.ksef.mf.gov.pl -- open session, SEND one FA(3) invoice, then READ it
# back by its KSeF-assigned number (UPO retrieval) -- and proves the bytes
# survive both directions. It reuses the ksef-demo FULL runner; this wrapper
# adds the discipline the seed calls for:
#
#   * FAIL-CLOSED. No credentials for the demo env => BLOCK (exit 3), a
#     distinct outcome from a real test failure. Sandbox unavailable is never
#     silently a pass.
#   * WRITE->READ ASSERTED. A green run that SKIPPED the send is not proof.
#     Success requires the send marker AND the read-back-by-KSeF-number marker,
#     on top of the demo's own aggregate exit code.
#   * SIDE EFFECTS ACKNOWLEDGED. FULL sends a REAL invoice to the demo env and
#     the server imposes a ~30-60s per-NIP session cooldown ("run once per
#     NIP"). The send is gated behind explicit confirmation.
#
# Exit codes:  0 = write->read PROVEN   1 = ran but NOT proven   3 = BLOCKED
#
# Usage:
#   context/tooling/live-e2e.sh            # preflight + confirm prompt
#   context/tooling/live-e2e.sh --yes      # skip the confirm prompt (CI/agent)
#   KSEF_E2E_CONFIRM=1 context/tooling/live-e2e.sh

set -u
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO" || exit 1

CREDS_FILE="$REPO/ksef-credentials.properties"
TS="$(date +%Y%m%d-%H%M%S)"
LOG="/tmp/ksef-e2e-${TS}.log"

SENT_MARKER="sent invoice ref="
READBACK_MARKER="UPO by KSeF number retrieved"
FAIL_MARKER="UNEXPECTED FAIL"

confirm=0
for arg in "$@"; do
    [ "$arg" = "--yes" ] && confirm=1
done
[ "${KSEF_E2E_CONFIRM:-}" = "1" ] && confirm=1

# --- 1. Credential preflight: absent => BLOCK, not a run -------------------
have_creds=0
if [ -f "$CREDS_FILE" ]; then
    have_creds=1
    creds_source="ksef-credentials.properties"
elif [ -n "${KSEF_NIP:-}" ] && \
     { [ -n "${KSEF_TOKEN:-}" ] || [ -n "${KSEF_TOKEN_READ:-}" ] || \
       [ -n "${KSEF_CERT_PATH:-}" ]; }; then
    have_creds=1
    creds_source="environment (KSEF_NIP + token/cert)"
fi

if [ "$have_creds" -eq 0 ]; then
    cat >&2 <<'EOF'
BLOCKED -- no credentials for the KSeF demo environment.

The live write->read needs demo-env auth, provided by EITHER:
  * ksef-credentials.properties in the repo root (gitignored), OR
  * env vars KSEF_NIP plus one of KSEF_TOKEN / KSEF_TOKEN_READ / KSEF_CERT_PATH

This is BLOCKED, not FAILED: sandbox unavailable is not a passing gate and
must not be reported as one. Provide credentials and re-run.
EOF
    exit 3
fi
echo "Credentials: $creds_source"

# --- 2. Side-effect gate: FULL sends a real invoice -----------------------
if [ "$confirm" -eq 0 ]; then
    cat >&2 <<'EOF'
This runs ksef-demo in FULL mode, which SENDS ONE REAL FA(3) invoice to
api-demo.ksef.mf.gov.pl and then reads it back. The demo server imposes a
~30-60s per-NIP session cooldown afterwards ("run once per NIP").

Re-run with --yes (or KSEF_E2E_CONFIRM=1) to proceed.
EOF
    exit 3
fi

# --- 3. Run FULL, capture to /tmp (run once, grep the file) ---------------
echo "Running live write->read (FULL) -> $LOG"
./gradlew --console=plain :ksef-demo:run -Pdemo.mode=FULL >"$LOG" 2>&1
demo_exit=$?

# --- 4. Assert the round trip actually happened ---------------------------
sent=$(grep -c "$SENT_MARKER" "$LOG")
readback=$(grep -c "$READBACK_MARKER" "$LOG")
ksef_number=$(grep "$SENT_MARKER" "$LOG" | head -1 | sed 's/.*ref=//')

echo
echo "demo exit code : $demo_exit"
echo "send markers   : $sent   ($SENT_MARKER)"
echo "readback markers: $readback   ($READBACK_MARKER)"
[ -n "$ksef_number" ] && echo "invoice ref    : $ksef_number"

if [ "$demo_exit" -eq 0 ] && [ "$sent" -ge 1 ] && [ "$readback" -ge 1 ]; then
    echo
    echo "WRITE->READ PROVEN -- invoice sent and read back through the SDK."
    exit 0
fi

echo
echo "NOT PROVEN -- write->read did not complete. Triage:"
if grep -q "$FAIL_MARKER" "$LOG"; then
    grep "$FAIL_MARKER" "$LOG" | head -5
fi
if [ "$sent" -eq 0 ]; then
    echo "  * no send marker: the invoice was never sent (auth/session failure,"
    echo "    or cooldown -- wait ~60s and retry)."
elif [ "$readback" -eq 0 ]; then
    echo "  * sent but not read back: UPO-by-KSeF-number retrieval did not run."
fi
echo "Full log: $LOG"
exit 1
