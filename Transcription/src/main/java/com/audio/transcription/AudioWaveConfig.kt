package com.audio.transcription

import android.media.AudioFormat
data class WaveConfig(
    var sampleRate: Int = 48000,
    var channels: Int = AudioFormat.CHANNEL_IN_MONO,
    var audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
)

internal fun bitPerSample(audioEncoding: Int) = when (audioEncoding) {
    AudioFormat.ENCODING_PCM_8BIT -> 8
    AudioFormat.ENCODING_PCM_16BIT -> 16
    else -> 16
}