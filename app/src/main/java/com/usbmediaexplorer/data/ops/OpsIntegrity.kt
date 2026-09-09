package com.usbmediaexplorer.data.ops

import java.io.InputStream
import java.security.MessageDigest

object OpsIntegrity {
    const val MAX_HASH_BYTES = 64L * 1024 * 1024

    fun sha256(input: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        input.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }

    fun matches(left: InputStream, right: InputStream): Boolean = sha256(left).contentEquals(sha256(right))
}
