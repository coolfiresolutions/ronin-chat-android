package com.coolfiresolutions.roninchat.server

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

object DeviceInfoUtility {
    private var id: String? = null
    private const val INSTALLATION_ID = "INSTALLATION_ID"

    val deviceName: String
        get() = Build.MODEL

    val userDeviceName: String
        get() {
            var name = "Unknown RONIN Device"
            val myDevice = BluetoothAdapter.getDefaultAdapter()
            if (myDevice != null) {
                name = myDevice.name
            }
            return name
        }

    val deviceOS: String
        get() = Build.VERSION.RELEASE

    fun getDeviceID(context: Context): String {
        if (id == null) {
            val installation = File(context.filesDir,
                    INSTALLATION_ID
            )
            try {
                if (!installation.exists()) {
                    writeInstallationFile(installation)
                }
                id = readInstallationFile(installation)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }
        return id!!
    }

    @Throws(IOException::class)
    private fun readInstallationFile(installation: File): String {
        val randomAccessFile = RandomAccessFile(installation, "r")
        val bytes = ByteArray(randomAccessFile.length().toInt())
        randomAccessFile.readFully(bytes)
        randomAccessFile.close()
        return String(bytes)
    }

    @Throws(IOException::class)
    private fun writeInstallationFile(installation: File) {
        val out = FileOutputStream(installation)
        var id: String? = null
        if (id == null) {
            id = UUID.randomUUID().toString()
        }
        out.write(id.toByteArray())
        out.close()
    }
}

