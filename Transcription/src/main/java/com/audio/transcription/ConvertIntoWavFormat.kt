package com.audio.transcription


import android.app.Activity
import android.media.*
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream




class ConvertIntoWavFormat(activity: Activity, fileName: String) {
    private var outputDirectory: File
    private var outputFile: File? = null

    init {
        outputDirectory = Utils.outputDirectoryInCache(activity, "Convert to wav")
        val wavFileName = fileName.substring(0, fileName.lastIndexOf("."))
        outputFile = File(outputDirectory, "$wavFileName.wav")
    }

    suspend fun convertingAudioToWav(inputFilePath: String,mimeType:String, successFullyConvertListener: (File) -> Unit, failToConvertListener: () -> Unit) {
        try {
            outputFile?.let {
                withContext(Dispatchers.IO) {
                    convertToWAV(inputFilePath, it,mimeType, successFullyConvertListener, failToConvertListener)
                }
            }
        } catch (ex: Exception) {
            failToConvertListener.invoke()
        }
    }

    private fun convertToWAV(
        inputFilePath: String,
        outputFile: File,mimeType:String,
        successFullyConvertListener: (File) -> Unit,
        failToConvertListener: () -> Unit
    ): Boolean {
        return try {
            val extractor = MediaExtractor()
            inputFilePath.let { extractor.setDataSource(it) }
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    extractor.selectTrack(audioTrackIndex)
                    break
                }
            }
            if (audioTrackIndex == -1) {
                Log.e("decodeAAC**", "No audio track found in input file")
                return false
            }

            val audioFormat = MediaFormat.createAudioFormat(
                mimeType,
                AudioUtils.getSampleRateOfAudio(inputFilePath.toUri())[0].sampleRate,
                AudioUtils.getSampleRateOfAudio(inputFilePath.toUri())[0].channels
            )
            audioFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            val codec = MediaCodec.createDecoderByType(
                extractor.getTrackFormat(audioTrackIndex).getString(
                    MediaFormat.KEY_MIME
                )!!
            )
            codec.configure(extractor.getTrackFormat(audioTrackIndex), null, null, 0)
            codec.start()
            val output = FileOutputStream(outputFile)

            var done = false
            while (!done) {
                // Read the input audio data
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                    val sampleSize = inputBuffer?.let { extractor.readSampleData(it, 0) }
                    if (sampleSize != null) {
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            done = true
                        } else {
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0
                            )
                            extractor.advance()
                        }
                    }
                }

                val bufferInfo = MediaCodec.BufferInfo()
                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    val outputData = ByteArray(bufferInfo.size)
                    outputBuffer?.get(outputData)
                    output.write(outputData)
                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                }
            }

            codec.stop()
            output.close()

            AudioWaveHeaderWriter(outputFile, null, AudioUtils.getSampleRateOfAudio(inputFilePath.toUri())[0].sampleRate.toLong(), AudioUtils.getSampleRateOfAudio(inputFilePath.toUri())[0].channels, AudioFormat.ENCODING_PCM_16BIT).writeHeader()
            successFullyConvertListener.invoke(outputFile)
            true
        } catch (e: Exception) {
            failToConvertListener.invoke()
            e.message?.let { Log.e("decodeAAC**", it) }
            false
        }
    }






}
