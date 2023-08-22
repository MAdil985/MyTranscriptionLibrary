package com.audio.transcription

import android.annotation.SuppressLint
import android.content.*
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*


object Utils {

    @Throws(IOException::class)
    fun getBytes(inputStream: InputStream): ByteArray? {
        val byteBuffer = ByteArrayOutputStream()
        val bufferSize = 1024
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            byteBuffer.write(buffer, 0, len)
        }
        return byteBuffer.toByteArray()
    }


    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    fun getFileExtentionWithFileName(filename: String): String {
        return filename.takeLast(4).replace(".", "")
    }
    fun getFileNameForGoogleCloudStorage(filename: String):String {
        Log.d("filename**",filename.substringAfterLast("/"))
        return filename.substringAfterLast("/")
    }

    fun getUriFromIntent(stringExtra: String?): Uri? {
        return if (stringExtra!!.startsWith("content://")) {
            stringExtra.toUri()

        } else {
            Uri.fromFile(File(stringExtra))

        }
    }

    fun outputDirectoryInCache(context: Context, folderName: String): File {
        val mediaDir = context.externalMediaDirs.first()?.let { mFile ->

            File(mFile, folderName).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            context.filesDir
        }
    }

    @SuppressLint("DefaultLocale")
    fun getDuration(context: Context, data: Uri): Long? {
        val durationStr = try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, data)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        } catch (e: Exception) {
            0.toString()
        }
        return durationStr?.toLong()
    }


    fun extractFilePathFromUrl(url: String): String? {
        try {
            val uri = Uri.parse(url)
            return uri.path
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}