package com.eb.obd2.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eb.obd2.models.ConnectionStatus
import com.eb.obd2.models.DrivingState
import com.eb.obd2.models.RenderStatus
import com.eb.obd2.models.RuntimeRecord
import com.eb.obd2.repositories.RecordRepository
import com.eb.obd2.services.OBDConnectionFactory
import com.eb.obd2.services.OBDService
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class OBDViewModel @Inject constructor(
    private val obdService: OBDService,
    private val factory: OBDConnectionFactory,
    private val repository: RecordRepository
) : ViewModel() {

    /** The status of view model. */
    var status: RenderStatus by mutableStateOf(RenderStatus.LOADING)
        private set

    /** The FSM driving state */
    var drivingState: DrivingState by mutableStateOf(DrivingState.IDLE)
        private set

    /** The comfort level [0.0, 1.0] 0 is really bad, 1.0 is comfortable */
    var comfortLevel: Float by mutableFloatStateOf(1.0f)
        private set

    /** The latest acceleration */
    var acceleration: Float by mutableFloatStateOf(0.0f)
        private set

    /** The pid key with record map. */
    val obdData = mutableStateMapOf<String, MutableList<RuntimeRecord>>()

    /** The aggressive score list */
    val aggressiveScores = mutableStateListOf(0.0f)

    /** The chart model producer */
    val modelProducer = CartesianChartModelProducer.build()

    private val maxBufferCapacity = 32

    private val aggressiveWeight = 0.9f
    private val nonAggressiveWeight = 0.975f

    private var drivingStateStartTime: LocalDateTime? = null

    init {
        obdService.startConnection()
        viewModelScope.launch {
            launch {
                obdService.connectionState.collect {
                    status = when (it) {
                        ConnectionStatus.CONNECTING -> RenderStatus.LOADING
                        ConnectionStatus.CONNECTED -> RenderStatus.SUCCESS
                        ConnectionStatus.DISCONNECTED -> RenderStatus.ERROR
                        ConnectionStatus.FAILED -> RenderStatus.ERROR
                    }
                }
            }

            launch {
                obdService.obdData.collect {
                    if (shouldUpdateData(it.command, it)) {
                        val record = repository.saveRecord(it)

                        obdData[it.command]?.let { list ->
                            obdData[it.command] = list.toMutableList().apply {
                                add(record)
                                if (size > maxBufferCapacity) {
                                    removeAt(0)
                                }
                            }
                        } ?: run {
                            obdData[it.command] = mutableListOf(record)
                        }

                        when(it.command) {
                            "0D" -> {
                                val speedRecord = record as RuntimeRecord.SpeedRecord
                                acceleration = speedRecord.acceleration
                                analyseStyle(speedRecord.speed, speedRecord.acceleration)
                            }
                        }
                    }
                }
            }

            launch {
                while (true) {
                    // confront aggressive
                    calculateAggressive(0.0f, false)

                    modelProducer.tryRunTransaction {
                        lineSeries {
                            series(aggressiveScores)
                        }
                    }

                    delay(1000)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        obdService.stopConnection()
    }

    private fun shouldUpdateData(pidKey: String, newData: RuntimeRecord): Boolean {
        val lastRecord = obdData[pidKey]?.lastOrNull() ?: return true
        return lastRecord.value != newData.value
    }

    private fun analyseStyle(speed: Float, acceleration: Float) {
        val currentTime = LocalDateTime.now()

        when (drivingState) {
            DrivingState.IDLE -> {
                if (acceleration > 5.7f) {
                    drivingState = DrivingState.AGGRESSIVE_ACCELERATION
                    drivingStateStartTime = currentTime
                    comfortLevel = 1.0f
                }
            }
            DrivingState.NORMAL -> {
                if (speed == 0.0f) drivingState = DrivingState.IDLE
                comfortLevel = 1.0f
            }
            DrivingState.AGGRESSIVE_ACCELERATION -> {
                if (acceleration < -7.5f) {
                    val stateElapsed = Duration.between(drivingStateStartTime, currentTime).seconds
                    if (stateElapsed <= 3) comfortLevel = 0.0f
                    drivingState = DrivingState.NORMAL
                }
            }
            DrivingState.AGGRESSIVE_DECELERATION -> {
                drivingState = DrivingState.NORMAL
                comfortLevel = 0.0f
            }
        }

        val isAggressive = acceleration in -7.5f..5.7f

        calculateAggressive(if (isAggressive) 1.0f else 0.0f, isAggressive)
    }

    private fun calculateAggressive(acceleration: Float, isAggressive: Boolean) {
        val weight = if (isAggressive) aggressiveWeight else nonAggressiveWeight

        if (aggressiveScores.size > maxBufferCapacity) {
            aggressiveScores.removeAt(0)
        }

        aggressiveScores.add(aggressiveScores.last() * weight + acceleration * (1.0f - weight))
    }
}
