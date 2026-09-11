package com.usbmediaexplorer

import androidx.media3.common.PlaybackException
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

    // --- BUG-01: the Media3 error code is the contract, text is only the fallback ---

    @Test fun classifiesByMedia3ErrorCode() {
        assertEquals(
            PlaybackFailure.PERMISSION_REVOKED,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_NO_PERMISSION),
        )
        assertEquals(
            PlaybackFailure.SOURCE_UNAVAILABLE,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND),
        )
        assertEquals(
            PlaybackFailure.NETWORK_NOT_APPLICABLE,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT),
        )
        assertEquals(
            PlaybackFailure.DECODER_UNSUPPORTED,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED),
        )
        assertEquals(
            PlaybackFailure.UNKNOWN,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_UNSPECIFIED),
        )
    }

    /** Regression: "reSOURCEs" used to match the loose SOURCE rule of the text classifier. */
    @Test fun decoderResourceReclaimIsNotReportedAsMissingSource() {
        assertEquals(
            PlaybackFailure.DECODER_UNSUPPORTED,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED),
        )
        assertEquals(
            PlaybackFailure.DECODER_UNSUPPORTED,
            PlaybackFailure.from("ERROR_CODE_DECODING_RESOURCES_RECLAIMED", null),
        )
    }

    /** Regression: the old `IO_` substring rule swallowed every network code. */
    @Test fun networkIoCodesAreNotReportedAsMissingSource() {
        assertEquals(
            PlaybackFailure.NETWORK_NOT_APPLICABLE,
            PlaybackFailure.from(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED),
        )
        assertEquals(
            PlaybackFailure.NETWORK_NOT_APPLICABLE,
            PlaybackFailure.from("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", null),
        )
        assertEquals(
            PlaybackFailure.NETWORK_NOT_APPLICABLE,
            PlaybackFailure.from("ERROR_CODE_IO_BAD_HTTP_STATUS", null),
        )
    }

    /** A missing file must stay a missing file, whatever the wording around it. */
    @Test fun missingSourceBeatsDecoderWordingInMessages() {
        assertEquals(
            PlaybackFailure.SOURCE_UNAVAILABLE,
            PlaybackFailure.from(null, "java.io.FileNotFoundException: no such file or directory"),
        )
    }
}
