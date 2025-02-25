package com.eb.obd2.services

import android.content.Context
import android.content.pm.PackageManager
import com.eb.obd2.R
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

enum class OBDConnectionType : OBDConnection {
    TCP {
        private lateinit var socket: Socket
        private var host: String = "10.0.2.2" // Default emulator host
        private var port: Int = 3000 // Default development port

        override fun setup(context: Context) {
            if (context.checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
                throw Error("Require internet access permission.")

            host = context.getString(R.string.server_host)
            port = context.resources.getInteger(R.integer.server_port)
        }

        override fun openConnection() {
            socket = Socket(host, port)
        }

        override fun closeConnection() {
            if (this::socket.isInitialized) {
                socket.close()
            }
        }

        override val inputStream: InputStream
            get() = socket.getInputStream()

        override val outputStream: OutputStream
            get() = socket.getOutputStream()

    },
    COM {
        override fun openConnection() {
            TODO("Not yet implemented")
        }

        override fun closeConnection() {
            TODO("Not yet implemented")
        }

        override val inputStream: InputStream
            get() = TODO("Not yet implemented")

        override val outputStream: OutputStream
            get() = TODO("Not yet implemented")

    }
}