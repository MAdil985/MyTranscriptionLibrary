package com.audio.transcription


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.longrunning.OperationFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.*
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.protobuf.ByteString
import java.io.FileInputStream

fun readCredentialFile(context: Context,creFileName:String): GoogleCredentials? {
    return GoogleCredentials.fromStream(context.resources.assets.open(creFileName))
}

fun setRecognitionConfig(fileName: String, fileExtension: String, sampleRate: Int, channels: Int): RecognitionConfig? {
    val languageList = ArrayList<String>()
    languageList.add("ar-SA")
    languageList.add("zh")
    languageList.add("da-DK")
    languageList.add("en-US")
    languageList.add("hi-IN")
    languageList.add("ja-JP")
    languageList.add("ru-RU")
    languageList.add("es-ES")
    languageList.add("ur-PK")
    return if (fileName.lowercase().trim().contains("translated audio")) {
        RecognitionConfig.newBuilder()
            .setAudioChannelCount(channels)
            .setModel("default")
            .setEncoding(AudioEncoding.LINEAR16)
            .addAllAlternativeLanguageCodes(languageList)
            .setLanguageCode("en-US")
            .setEnableAutomaticPunctuation(true)
            .setSampleRateHertz(sampleRate)
            .build()
    } else {
        when (fileExtension) {
            "wav" -> {
                RecognitionConfig.newBuilder()
                    .setAudioChannelCount(channels)
                    .setModel("default").setEncoding(AudioEncoding.LINEAR16)
                    .addAllAlternativeLanguageCodes(languageList)
                    .setLanguageCode("en-US")
                    .setEnableAutomaticPunctuation(true)
                    .setSampleRateHertz(sampleRate)
                    .build()
            }

            "flac" -> {
                RecognitionConfig.newBuilder()
                    .setAudioChannelCount(channels)
                    .setModel("default").setEncoding(AudioEncoding.FLAC)
                    .setLanguageCode("en").setEnableAutomaticPunctuation(true)
                    .setSampleRateHertz(sampleRate)
                    .build()
            }

            "opus" -> {
                RecognitionConfig.newBuilder().setAudioChannelCount(1)
                    .setModel("default").setEncoding(AudioEncoding.OGG_OPUS)
                    .setLanguageCode("en").setEnableAutomaticPunctuation(true)
                    .setSampleRateHertz(16000).build()
            }

            else -> {
                RecognitionConfig.newBuilder()
                    .setAudioChannelCount(channels)
                    .setModel("default").setEncoding(AudioEncoding.LINEAR16)
                    .addAllAlternativeLanguageCodes(languageList)
                    .setLanguageCode("en-US")
                    .setEnableAutomaticPunctuation(true)
                    .setSampleRateHertz(sampleRate)
                    .build()
            }
        }
    }

}

fun getResponseIntoString(context: Context, audio: RecognitionAudio, speech: SpeechClient, config: RecognitionConfig): String {
    var transcriptedOutput = ""
    val response: OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> =
        speech.longRunningRecognizeAsync(config, audio)
    val startTime = System.currentTimeMillis()



    while (!response.isDone) {
        Log.d("response**", "Waiting for response...")
        Thread.sleep(10000)
        if (!Utils.isOnline(context)){
            throw Exception("No internet")
        }
    }

    val results: List<SpeechRecognitionResult> = response.get().resultsList
    for (result in results) {
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val alternative: SpeechRecognitionAlternative = result.alternativesList[0]
        transcriptedOutput = alternative.transcript
        Log.d("Transcription**", result.toString()+totalTime)
    }

    return transcriptedOutput

}

fun transcriptionLessThenMinuteAudio(context: Context, filePath: Uri, fileName: String, fileExtension: String, sampleRate: Int, channels: Int, creFileName: String): String {
    var transcriptedOutput: String
    try {
        val credentialsProvider: CredentialsProvider =
            FixedCredentialsProvider.create(readCredentialFile(context,creFileName))
        val speechSettings: SpeechSettings =
            SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()

        SpeechClient.create(speechSettings).use { speech ->
            val iStream =FileInputStream(filePath.toString())
            val inputData: ByteArray? = iStream.let { Utils.getBytes(it) }
            val audioBytes: ByteString = ByteString.copyFrom(inputData)
            val audio: RecognitionAudio =
                RecognitionAudio.newBuilder().setContent(audioBytes).build()
            transcriptedOutput = setRecognitionConfig(
                fileName,
                fileExtension,
                sampleRate,
                channels
            )?.let { getResponseIntoString(context,audio, speech, it) }.toString()

        }
    } catch (e: Exception) {
        Log.d("google**", e.toString())
        e.printStackTrace()
        transcriptedOutput = if (checkExceptionType(e)) "No internet" else ""

    }
    return transcriptedOutput
}

fun uploadObjectToGoogleCloudAndTranscribe(context: Context, bucketName: String, fileName: String, filePath: Uri?, fileExtension: String, sampleRate: Int, channels: Int,creFileName:String): String {
    var transcriptedOutput: String
    try {
        val storageOptions =
            StorageOptions.newBuilder().setCredentials(readCredentialFile(context,creFileName)).build()
        val storage = StorageOptions.newBuilder().setCredentials(readCredentialFile(context,creFileName)).setProjectId(storageOptions.projectId).build().service
        val blobId = BlobId.of(bucketName, fileName)
        val blobInfo = BlobInfo.newBuilder(blobId).build()
        val iStream: FileInputStream? = filePath?.let { FileInputStream(it.toString()) }
        val precondition: Storage.BlobWriteOption = if (storage[bucketName, fileName] == null) {
            Storage.BlobWriteOption.doesNotExist()
        } else {
            Storage.BlobWriteOption.generationMatch(storage[bucketName, fileName].generation)
        }
        storage.createFrom(blobInfo, iStream, precondition)
        val gcsUri = ("gs://$bucketName/$fileName").toUri()
        transcriptedOutput = transcriptionLongerThenMinuteAudio(
            context,
            gcsUri,
            fileName,
            fileExtension,
            sampleRate,
            channels,
            creFileName
        )
        Log.d("uploadObject**", "$gcsUri")
    } catch (e: Exception) {
        Log.d("uploadObject**", e.toString())
        transcriptedOutput = if (checkExceptionType(e)) "No internet" else ""
    }
    return transcriptedOutput
}

fun transcriptionLongerThenMinuteAudio(context: Context, filePath: Uri, fileName: String, fileExtension: String, sampleRate: Int, channels: Int,creFileName:String): String {
    var transcriptedOutput: String

    try {
        val credentialsProvider: CredentialsProvider =
            FixedCredentialsProvider.create(readCredentialFile(context,creFileName))
        val speechSettings: SpeechSettings =
            SpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build()
        SpeechClient.create(speechSettings).use { speech ->
            val audio: RecognitionAudio =
                RecognitionAudio.newBuilder().setUri(filePath.toString()).build()
            transcriptedOutput = setRecognitionConfig(
                fileName,
                fileExtension,
                sampleRate,
                channels
            )?.let { getResponseIntoString(context,audio, speech, it) }.toString()

        }
    } catch (e: Exception) {
        Log.d("google**", e.toString())
        transcriptedOutput = if (checkExceptionType(e)) "No internet" else ""
    }
    return transcriptedOutput
}

private fun checkExceptionType(ex: Exception):Boolean {
    return ex.toString().trim().contains("No internet",ignoreCase = true)
}