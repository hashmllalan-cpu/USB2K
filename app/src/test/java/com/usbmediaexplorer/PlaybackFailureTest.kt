package com.usbmediaexplorer

import androidx.media3.common.PlaybackException
import com.usbmediaexplorer.ui.player.PlaybackFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureTest {
    @Test fun classifiesDecoderErrors() {
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from("ERROR_CODE_DECODER_INIT_FAILED", "codec"))
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED))
    }

    @Test fun classifiesPermissionErrors() {
        assertEquals(PlaybackFailure.PERMISSION_REVOKED, PlaybackFailure.from("ERROR_CODE_IO_NO_PERMISSION", ""))
        assertEquals(PlaybackFailure.PERMISSION_REVOKED, PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_NO_PERMISSION))
    }

    @Test fun classifiesMissingSourceErrors() {
        assertEquals(PlaybackFailure.SOURCE_UNAVAILABLE, PlaybackFailure.from("ERROR_CODE_IO_FILE_NOT_FOUND", "missing"))
        assertEquals(PlaybackFailure.SOURCE_UNAVAILABLE, PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
    }

    @Test fun doesNotMisclassifyResourceExhaustionAsSourceUnavailable() {
        // BUG-01 regression: "RESOURCE" contains "SOURCE", but must classify as DECODER_UNSUPPORTED/resource failure
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from("ERROR_CODE_DECODER_INIT_FAILED", "RESOURCE_EXHAUSTED"))
        assertEquals(PlaybackFailure.DECODER_UNSUPPORTED, PlaybackFailure.from("MEDIA_CODEC_ERROR", "RESOURCE_EXHAUSTED"))
    }

    @Test fun doesNotExposeUnknownTechnicalTextAsCategory() {
        assertEquals(PlaybackFailure.UNKNOWN, PlaybackFailure.from("ERROR_CODE_UNSPECIFIED", "unexpected"))
    }
}
