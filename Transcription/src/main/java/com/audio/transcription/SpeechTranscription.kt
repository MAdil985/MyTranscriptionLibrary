package com.audio.transcription


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.audio.transcription.Utils.extractFilePathFromUrl
import com.audio.transcription.Utils.getFileNameForGoogleCloudStorage


class SpeechTranscription(private var credentialFileName: String?, private var googleBucketName: String) {

    fun getTextFromAudioFile(
        activity: Activity,
        data: Uri,
        audioFileName: String,
        transcriptionCompleteListener: (String) -> Unit
    ) {
         CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioDuration: Long? = Utils.getDuration(activity, data)
                if (audioDuration != null) {
                    if (Utils.getFileExtentionWithFileName(audioFileName).contains("aac")) {
                        withContext(Dispatchers.IO) {
                            ConvertIntoWavFormat(
                                activity, audioFileName
                            ).convertingAudioToWav(data.toString(), "audio/raw", {
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (extractFilePathFromUrl(it.path) != null) {


                                        startTranscription(
                                            activity,
                                            extractFilePathFromUrl(it.path)!!.toUri(),
                                            getFileNameForGoogleCloudStorage(
                                                extractFilePathFromUrl(
                                                    it.path
                                                )!!
                                            ),
                                            audioDuration,
                                            transcriptionCompleteListener
                                        )
                                    }


                                }
                            }) {
                                AudioUtils.transcriptedOutput = ""
                                transcriptionCompleteListener("")
                            }
                        }
                    }
                    else if (Utils.getFileExtentionWithFileName(audioFileName).contains("mp3")) {
                        withContext(Dispatchers.IO) {

                            ConvertIntoWavFormat(
                                activity, audioFileName
                            ).convertingAudioToWav(data.toString(), "audio/mpeg", {
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (extractFilePathFromUrl(it.path) != null) {
                                        startTranscription(
                                            activity,
                                            extractFilePathFromUrl(it.path)!!.toUri(),
                                            getFileNameForGoogleCloudStorage(
                                                extractFilePathFromUrl(it.path)!!
                                            ),
                                            audioDuration,
                                            transcriptionCompleteListener
                                        )
                                    }

                                }
                            }) {
                                AudioUtils.transcriptedOutput = ""
                                transcriptionCompleteListener("")
                            }
                        }
                    }
                    else if (Utils.getFileExtentionWithFileName(audioFileName).contains("ogg")) {
                        withContext(Dispatchers.IO) {

                            ConvertIntoWavFormat(
                                activity, audioFileName
                            ).convertingAudioToWav(data.toString(), "audio/ogg", {
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (extractFilePathFromUrl(it.path) != null) {
                                        startTranscription(
                                            activity,
                                            extractFilePathFromUrl(it.path)!!.toUri(),
                                            getFileNameForGoogleCloudStorage(
                                                extractFilePathFromUrl(it.path)!!
                                            ),
                                            audioDuration,
                                            transcriptionCompleteListener
                                        )
                                    }

                                }
                            }) {
                                AudioUtils.transcriptedOutput = ""
                                transcriptionCompleteListener("")
                            }
                        }
                    }
                    else {
                        startTranscription(
                            activity,
                            data,
                            audioFileName,
                            audioDuration,
                            transcriptionCompleteListener
                        )
                    }
                }
            }
            catch (ex: Exception) {
                transcriptionCompleteListener(if (checkExceptionType(ex)) "No internet"   else "")


            }
        }
    }


    private fun checkExceptionType(ex: Exception):Boolean {
        return ex.toString().trim().contains("No internet",ignoreCase = true)
    }

    private suspend fun startTranscription(
        activity: Activity,
        data: Uri,
        audioFileName: String,
        audioDuration: Long?,
        transcriptionCompleteListener: (String) -> Unit
    ) {
        if (credentialFileName?.isNotEmpty() == true) {
            if (audioDuration != null) {
                AudioUtils.transcriptedOutput = if (audioDuration < 55000) {
                    transcriptionLessThenMinuteAudio(
                        activity,
                        data,
                        audioFileName,
                        Utils.getFileExtentionWithFileName(audioFileName),
                        AudioUtils.getSampleRateOfAudio(data)[0].sampleRate,
                        AudioUtils.getSampleRateOfAudio(data)[0].channels,
                        credentialFileName!!
                    )
                } else {
                    uploadObjectToGoogleCloudAndTranscribe(
                        activity,
                        googleBucketName,
                        getFileNameForGoogleCloudStorage(audioFileName),
                        data,
                        Utils.getFileExtentionWithFileName(audioFileName),
                        AudioUtils.getSampleRateOfAudio(data)[0].sampleRate,
                        AudioUtils.getSampleRateOfAudio(data)[0].channels,
                        credentialFileName!!
                    )
                }


                transcriptionCompleteListener(if ( AudioUtils.transcriptedOutput=="No internet")   throw Exception("No internet") else AudioUtils.transcriptedOutput)


            }
        } else {
            Toast.makeText(activity, "creFilename is empty", Toast.LENGTH_LONG).show()
            transcriptionCompleteListener("")
        }
    }

}






