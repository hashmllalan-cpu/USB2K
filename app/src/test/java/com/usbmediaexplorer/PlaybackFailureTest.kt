package com.usbmediaexplorer

import com.usbmediaexplorer.ui.player.PlaybackFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureTest {
    @Test fun classifiesDecoderErrors() {
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from("ERROR_CODE_DECODER_INIT_FAILED", "codec"))
    }

    @Test fun classifiesPermissionErrors() {
        assertEquals(PlaybackFailure.PERMISSION_REVOKED, PlaybackFailure.from("ERROR_CODE_IO_NO_PERMISSION", ""))
    }

    @Test fun classifiesMissingSourceErrors() {
        assertEquals(PlaybackFailure.SOURCE_UNAVAILABLE, PlaybackFailure.from("ERROR_CODE_IO_FILE_NOT_FOUND", "missing"))
    }

    @Test fun doesNotExposeUnknownTechnicalTextAsCategory() {
        assertEquals(PlaybackFailure.UNKNOWN, PlaybackFailure.from("ERROR_CODE_UNSPECIFIED", "unexpected"))
    }
}
