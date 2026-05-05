/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.ksef.sdk.domain.invoicing.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Caller-prepared batch package for manual batch session control.
 *
 * <p>Bundles the on-the-wire metadata ({@link BatchFileSpec}) <strong>together
 * with the AES session key and IV that was used to encrypt the parts</strong>.
 * The SDK relies on this cryptographic contract:
 *
 * <ul>
 *   <li>{@code aesKey} is the 32-byte AES-256 key the caller used to encrypt
 *       every entry in {@code partBytes}.</li>
 *   <li>{@code initVector} is the 16-byte AES-CBC IV used for that encryption.</li>
 *   <li>{@code partBytes.get(i)} is the ciphertext of the {@code i}-th part
 *       described by {@code spec.parts().get(i)}.</li>
 * </ul>
 *
 * <p>The SDK takes the AES key + IV verbatim, RSA-encrypts the key with the
 * KSeF SymmetricKeyEncryption certificate, and registers the encryption
 * material with the open-batch-session request. If the caller's parts were
 * encrypted with a different key or IV, KSeF will reject the upload.
 *
 * <p>Most consumers should prefer the convenience overload
 * {@code openBatchSession(FormCode, List&lt;byte[]&gt;, BatchSessionOptions)}
 * which builds and encrypts the package internally — this manual entry
 * point exists for advanced flows that need streaming or external part
 * preparation.
 *
 * @param spec metadata describing the unencrypted batch file and the
 *             encrypted parts (sizes + SHA-256 hashes)
 * @param aesKey 32-byte AES-256 session key used to encrypt {@code partBytes}
 * @param initVector 16-byte AES-CBC IV used to encrypt {@code partBytes}
 * @param partBytes ciphertext of each part, in {@link BatchFileSpec.Part#ordinalNumber()} order
 */
public record PreparedBatchPackage(BatchFileSpec spec,
                                   byte[] aesKey,
                                   byte[] initVector,
                                   List<byte[]> partBytes) {

    private static final String ERR_SPEC_NULL = "spec must not be null";
    private static final String ERR_KEY_NULL = "aesKey must not be null";
    private static final String ERR_IV_NULL = "initVector must not be null";
    private static final String ERR_PARTS_NULL = "partBytes must not be null";
    private static final String ERR_PARTS_EMPTY = "partBytes must not be empty";
    private static final String ERR_PARTS_SIZE_MISMATCH =
            "partBytes count must match spec.parts() count";
    private static final int AES_256_KEY_LENGTH = 32;
    private static final int AES_CBC_IV_LENGTH = 16;
    private static final String ERR_KEY_LENGTH = "aesKey must be 32 bytes (AES-256)";
    private static final String ERR_IV_LENGTH = "initVector must be 16 bytes (AES-CBC)";

    public PreparedBatchPackage {
        Objects.requireNonNull(spec, ERR_SPEC_NULL);
        Objects.requireNonNull(aesKey, ERR_KEY_NULL);
        Objects.requireNonNull(initVector, ERR_IV_NULL);
        Objects.requireNonNull(partBytes, ERR_PARTS_NULL);
        if (aesKey.length != AES_256_KEY_LENGTH) {
            throw new IllegalArgumentException(ERR_KEY_LENGTH);
        }
        if (initVector.length != AES_CBC_IV_LENGTH) {
            throw new IllegalArgumentException(ERR_IV_LENGTH);
        }
        if (partBytes.isEmpty()) {
            throw new IllegalArgumentException(ERR_PARTS_EMPTY);
        }
        if (partBytes.size() != spec.parts().size()) {
            throw new IllegalArgumentException(ERR_PARTS_SIZE_MISMATCH);
        }
        aesKey = aesKey.clone();
        initVector = initVector.clone();
        partBytes = List.copyOf(partBytes);
    }

    @Override
    public byte[] aesKey() { return aesKey.clone(); }

    @Override
    public byte[] initVector() { return initVector.clone(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PreparedBatchPackage other)) {
            return false;
        }
        if (!Objects.equals(spec, other.spec)
                || !Arrays.equals(aesKey, other.aesKey)
                || !Arrays.equals(initVector, other.initVector)
                || partBytes.size() != other.partBytes.size()) {
            return false;
        }
        for (int index = 0; index < partBytes.size(); index++) {
            if (!Arrays.equals(partBytes.get(index), other.partBytes.get(index))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(spec, Arrays.hashCode(aesKey), Arrays.hashCode(initVector));
        for (byte[] part : partBytes) {
            result = 31 * result + Arrays.hashCode(part);
        }
        return result;
    }

    @Override
    public String toString() {
        return "PreparedBatchPackage[spec=" + spec
                + ", aesKey=" + aesKey.length + " bytes"
                + ", initVector=" + initVector.length + " bytes"
                + ", partBytes=" + partBytes.size() + " parts]";
    }
}
