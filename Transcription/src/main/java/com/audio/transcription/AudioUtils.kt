package com.audio.transcription


import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log


import java.io.IOException


object AudioUtils {
    var transcriptedOutput = ""

    fun getSampleRateOfAudio(fileUri: Uri): ArrayList<SampleRateModel> {
        val sampleRateList: ArrayList<SampleRateModel> = ArrayList()
        val mex = MediaExtractor()
        try {
            fileUri.path?.let { mex.setDataSource(it) }
            val mf = mex.getTrackFormat(0)
            val sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            sampleRateList.add(SampleRateModel(channelCount, sampleRate))
            Log.d("sampleRate**", "sampleRate: $sampleRate $channelCount")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("sampleRate**", "sampleRateError: $e")
        }
        return sampleRateList

    }



}