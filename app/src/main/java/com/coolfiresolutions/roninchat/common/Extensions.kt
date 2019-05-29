package com.coolfiresolutions.roninchat.common

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import org.joda.time.DateTime
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

private const val TAG = "extensions"

//Gravatar constants
private const val GRAVATAR_URL = "http://www.gravatar.com/avatar/"
private const val GRAVATAR_TRANSPARENT_PARAMETER = "?d=blank&s=200"

//Bitmap constants
private const val COMPRESSION_QUALITY = 50

private fun md5Hash(s: String): String {
    var m: MessageDigest? = null

    try {
        m = MessageDigest.getInstance("MD5")
    } catch (e: NoSuchAlgorithmException) {
        Log.e(TAG, e.message)
    }

    m!!.update(s.toByteArray(), 0, s.length)
    return BigInteger(1, m.digest()).toString(16)
}

fun String?.getGravatarUrlByEmail(): String? {
    if (this == null) {
        return null
    }
    val hash = md5Hash(this.toLowerCase().trim { it <= ' ' })
    return GRAVATAR_URL + hash + GRAVATAR_TRANSPARENT_PARAMETER
}

fun Bitmap.createFile(context: Context): File {
    val byteArrayOutputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, byteArrayOutputStream)
    val bitmapData = byteArrayOutputStream.toByteArray()

    val file = File(getNewFilePath(context, ".jpeg"))
    val fileOutputStream = FileOutputStream(file)
    fileOutputStream.write(bitmapData)
    fileOutputStream.flush()
    fileOutputStream.close()
    return file
}

private fun getNewFilePath(context: Context, extension: String): String {
    val dir = context.getExternalFilesDir(null)
    return ((if (dir == null) "" else dir.absolutePath + "/")
            + System.currentTimeMillis() + extension)
}


fun File.getMediaType(): String {
    val fileUri = Uri.fromFile(this)
    val extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString())
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)!!
}


fun DateTime.toFuzzyDateString(includeTimestamp: Boolean): String {
    val yesterday = DateTime().withTimeAtStartOfDay().minusDays(1)
    val today = DateTime().withTimeAtStartOfDay()

    val fuzzyDate = when {
        this.withTimeAtStartOfDay() == today -> "" //Intentionally left empty
        this.withTimeAtStartOfDay() == yesterday -> "Yesterday"
        getTimeDifferenceInDays(
                DateTime.now(), this) <= 7L -> this.toString("EEEE")
        else -> this.toString("MMM d")
    }

    val time = toLocalTime().toString("h:mm a")

    return when {
        fuzzyDate.isEmpty() -> //Today
            time
        includeTimestamp -> String.format("%s, %s", fuzzyDate, time)
        else -> fuzzyDate
    }
}

private fun getTimeDifferenceInDays(currentDate: DateTime, timeFromMessage: DateTime): Long {
    var timeDifference = currentDate.millis - timeFromMessage.millis
    timeDifference = TimeUnit.MILLISECONDS.toDays(timeDifference)
    return timeDifference
}
