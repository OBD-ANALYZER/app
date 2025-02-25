package com.eb.obd2.services

import android.content.Context
import javax.inject.Inject

class OBDConnectionFactory @Inject constructor(
    private val context: Context
) {

    fun create(connectionType: OBDConnectionType): OBDConnection {
        return connectionType.apply {
            setup(context)
        }
    }
}