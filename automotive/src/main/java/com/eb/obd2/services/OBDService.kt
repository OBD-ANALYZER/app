package com.eb.obd2.services

import android.util.Log
import com.eb.obd2.models.ConnectionStatus
import com.eb.obd2.models.RuntimeRecord
import com.github.pires.obd.commands.ObdCommand
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.engine.RPMCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class OBDService(private var connection: OBDConnection) {

    private val serviceScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serviceJob: Job? = null

    private val mutableConnectionStatus = MutableStateFlow(ConnectionStatus.CONNECTING)
    val connectionState: StateFlow<ConnectionStatus> = mutableConnectionStatus

    private val mutableObdData = MutableSharedFlow<RuntimeRecord.PlainRecord>()
    val obdData: SharedFlow<RuntimeRecord.PlainRecord> = mutableObdData

    private val commands = mutableSetOf<ObdCommand>()

    init {
        registerCommand(RPMCommand(), SpeedCommand())
    }

    fun registerCommand(vararg command: ObdCommand): OBDService {
        commands.addAll(command)
        return this
    }

    fun startConnection(retryTimes: Int = 20) = establishConnection(retryTimes)

    fun switchConnection(newConnection: OBDConnection, retryTimes: Int = 20) {
        stopConnection()
        connection = newConnection
        establishConnection(retryTimes)
    }

    private fun establishConnection(retryTimes: Int = 20) {
        serviceJob = serviceScope.launch {
            var attempts = 0

            while (attempts < retryTimes && mutableConnectionStatus.value != ConnectionStatus.CONNECTED) {
                try {
                    connection.openConnection()

                    mutableConnectionStatus.value = ConnectionStatus.CONNECTED

                    Log.i("OBD service", "OBD connected successfully.")

                    // The main service loop
                    while (mutableConnectionStatus.value == ConnectionStatus.CONNECTED) {
                        for (command in commands) {
                            command.run(connection.inputStream, connection.outputStream)
                            mutableObdData.emit(
                                RuntimeRecord.PlainRecord(
                                    command.commandPID,
                                    command.calculatedResult,
                                    command.resultUnit,
                                    LocalDateTime.now()
                                )
                            )
                        }
                    }

                    break
                } catch (e: Exception) {
                    attempts++
                    delay(5000)
                }
            }

            if (mutableConnectionStatus.value != ConnectionStatus.CONNECTED) {
                mutableConnectionStatus.value = ConnectionStatus.FAILED
            }
        }
    }

    fun stopConnection() {
        serviceJob?.cancel()
        connection.closeConnection()

        mutableConnectionStatus.value = ConnectionStatus.DISCONNECTED

        Log.i("OBD service", "OBD connection has been terminated.")
    }
}