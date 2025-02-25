package com.eb.obd2.services

import android.content.Context
import java.io.InputStream
import java.io.OutputStream

interface OBDConnection {
    /** Setup the connection (optional) */
    fun setup(context: Context) {}

    /** Start the connection */
    fun openConnection()

    /** Open the connection */
    fun closeConnection()

    val inputStream: InputStream

    val outputStream: OutputStream
}