package com.usbmediaexplorer.data.ops

import java.io.ByteArrayInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpsIntegrityTest {
    @Test fun `same streams have equal digest`() {
        assertTrue(OpsIntegrity.matches(ByteArrayInputStream(byteArrayOf(1, 2)), ByteArrayInputStream(byteArrayOf(1, 2))))
    }
    @Test fun `different streams fail integrity`() {
        assertFalse(OpsIntegrity.matches(ByteArrayInputStream(byteArrayOf(1)), ByteArrayInputStream(byteArrayOf(2))))
    }
}
