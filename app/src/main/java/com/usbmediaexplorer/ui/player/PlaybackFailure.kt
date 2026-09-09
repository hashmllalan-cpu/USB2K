package com.usbmediaexplorer.ui.player

import androidx.media3.common.PlaybackException

/** User-facing categories for recoverable playback failures. */
enum class PlaybackFailure {
    SOURCE_UNAVAILABLE,
    DECODER_UNSUPPORTED,
    PERMISSION_REVOKED,
    NETWORK_NOT_APPLICABLE,
    UNKNOWN;

    companion object {
        fun from(errorCode: Int): PlaybackFailure = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PERMISSION_REVOKED
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> SOURCE_UNAVAILABLE
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> NETWORK_NOT_APPLICABLE
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> DECODER_UNSUPPORTED
            else -> UNKNOWN
        }

        fun from(error: PlaybackException?): PlaybackFailure {
            if (error == null) return UNKNOWN
            val classified = from(error.errorCode)
            return if (classified != UNKNOWN) classified else from(error.errorCodeName, error.message)
        }

        fun from(errorCodeName: String?, message: String?): PlaybackFailure {
            val text = "${errorCodeName.orEmpty()} ${message.orEmpty()}".uppercase()
            return when {
                "PERMISSION" in text || "NO_PERMISSION" in text || "ACCESS_DENIED" in text -> PERMISSION_REVOKED
                "DECODER" in text || "DECOD" in text || "FORMAT" in text || "CODEC" in text -> DECODER_UNSUPPORTED
                "RESOURCE_EXHAUSTED" in text || "OUT_OF_MEMORY" in text -> DECODER_UNSUPPORTED
                "FILE_NOT_FOUND" in text || "NOT_FOUND" in text || Regex("\\bSOURCE\\b").containsMatchIn(text) || "IO_UNSPECIFIED" in text || "IO_FILE" in text -> SOURCE_UNAVAILABLE
                "NETWORK" in text -> NETWORK_NOT_APPLICABLE
                else -> UNKNOWN
            }
        }
    }
}
