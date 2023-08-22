package com.audio.transcription

import android.media.AudioFormat
import java.io.File
import java.io.RandomAccessFile

class AudioWaveHeaderWriter(private val filePath: File, private val waveConfig: WaveConfig?=null, private val sampleRate: Long?=null,
                            private val channelCount: Int?=null,
                            private val audioEncoding: Int?=null
) {

    fun writeHeader() {
        val inputStream =if (filePath.exists()) { filePath.inputStream() } else { return }

        val totalAudioLen = inputStream.channel.size() - 44
        val totalDataLen = totalAudioLen + 36

        val channels = if (waveConfig!=null) {
            if (waveConfig.channels == AudioFormat.CHANNEL_IN_MONO)
                1
            else
                2}else{channelCount}
        val sampleRate = waveConfig?.sampleRate?.toLong() ?: sampleRate
        val audioEncoding =waveConfig?.audioEncoding?: audioEncoding

        val byteRate = (((audioEncoding?.let { bitPerSample(it) }?.times(sampleRate!!)  )?.times(channels!!) !! ) / 8)
        val header = sampleRate?.let {
            getWavFileHeaderByteArray(totalAudioLen, totalDataLen, it, channels!!, byteRate, bitPerSample(audioEncoding))
        }

        val randomAccessFile = RandomAccessFile(filePath, "rw")
        randomAccessFile.seek(0)
        randomAccessFile.write(header)
        randomAccessFile.close()
    }

    private fun getWavFileHeaderByteArray(
        totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long,
        channels: Int, byteRate: Long, bitsPerSample: Int
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.toByte()
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        header[12] = 'f'.toByte()
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * (bitsPerSample / 8)).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        return header
    }
}