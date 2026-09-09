package com.usbmediaexplorer.ui.player

/** User-facing categories for recoverable playback failures. */
enum class PlaybackFailure {
    SOURCE_UNAVAILABLE,
    DECODER_UNSUPPORTED,
    PERMISSION_REVOKED,
    NETWORK_NOT_APPLICABLE,
    UNKNOWN,

    companion object {
        fun from(errorCodeName: String?, message: String?): PlaybackFailure {
            val text = "${errorCodeName.orEmpty()} ${message.orEmpty()}".uppercase()
            return when {
                "PERMISSION" in text || "NO_PERMISSION" in text || "ACCESS" in text -> PERMISSION_REVOKED
                "FILE_NOT_FOUND" in text || "NOT_FOUND" in text || "SOURCE" in text || "IO_" in text -> SOURCE_UNAVAILABLE
                "DECODER" in text || "DECOD" in text || "FORMAT" in text -> DECODER_UNSUPPORTED
                "NETWORK" in text -> NETWORK_NOT_APPLICABLE
                else -> UNKNOWN
            }
        }
    }
}
