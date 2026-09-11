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
        /**
         * Media3 error codes are the contract; text matching is only a fallback for errors
         * that arrive without a usable code.
         *
         * BUG-01: the classifier used to be text-only, which misread real cases —
         * `ERROR_CODE_DECODING_RESOURCES_RECLAIMED` contains the substring `SOURCE`
         * ("reSOURCEs") and was reported as a missing source, and every `ERROR_CODE_IO_*`
         * network code matched the broad `IO_` rule for the same reason.
         */
        fun from(errorCode: Int): PlaybackFailure = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> PERMISSION_REVOKED

            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> SOURCE_UNAVAILABLE

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> NETWORK_NOT_APPLICABLE

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> DECODER_UNSUPPORTED

            // UNSPECIFIED / TIMEOUT / BEHIND_LIVE_WINDOW / DRM: no honest user-facing
            // category, so the UI falls back to the generic retry screen.
            else -> UNKNOWN
        }

        /** Prefer the code, fall back to the text only when the code says nothing. */
        fun from(error: PlaybackException?): PlaybackFailure {
            if (error == null) return UNKNOWN
            val byCode = from(error.errorCode)
            return if (byCode != UNKNOWN) byCode else from(error.errorCodeName, error.message)
        }

        /**
         * Text fallback. Order matters: decoder/OOM signals are matched before the source
         * rules so that `..._RESOURCES_RECLAIMED` and `..._IO_NETWORK_*` cannot be swallowed
         * by a loose substring, and nothing here may invent a category out of technical text.
         */
        fun from(errorCodeName: String?, message: String?): PlaybackFailure {
            val text = "${errorCodeName.orEmpty()} ${message.orEmpty()}".uppercase()
            return when {
                "PERMISSION" in text || "NO_PERMISSION" in text || "ACCESS_DENIED" in text ->
                    PERMISSION_REVOKED

                "DECODER" in text || "DECOD" in text || "CODEC" in text ||
                    "FORMAT_UNSUPPORTED" in text || "EXCEEDS_CAPABILITIES" in text ||
                    "RESOURCE_EXHAUSTED" in text || "RESOURCES_RECLAIMED" in text ||
                    "OUT_OF_MEMORY" in text ||
                    "CONTAINER_UNSUPPORTED" in text || "MANIFEST_UNSUPPORTED" in text ->
                    DECODER_UNSUPPORTED

                "NETWORK" in text || "BAD_HTTP_STATUS" in text || "CLEARTEXT" in text ||
                    "INVALID_HTTP_CONTENT_TYPE" in text ->
                    NETWORK_NOT_APPLICABLE

                "FILE_NOT_FOUND" in text || "NOT_FOUND" in text || "NO_SUCH_FILE" in text ||
                    "FILENOTFOUND" in text || "ENOENT" in text ||
                    "IO_UNSPECIFIED" in text || "READ_POSITION_OUT_OF_RANGE" in text ->
                    SOURCE_UNAVAILABLE

                else -> UNKNOWN
            }
        }
    }
}
