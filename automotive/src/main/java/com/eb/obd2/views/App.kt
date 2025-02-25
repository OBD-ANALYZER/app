package com.eb.obd2.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.eb.obd2.models.RenderStatus
import com.eb.obd2.viewmodels.OBDViewModel
import com.eb.obd2.views.components.ErrorScreen
import com.eb.obd2.views.components.LoadingScreen
import com.eb.obd2.views.components.ScoreGraph
import com.eb.obd2.views.components.StyleIndicator

@Composable
fun App(viewModel: OBDViewModel = hiltViewModel()) {
    var showDebug by remember { mutableStateOf(false) }

    when (viewModel.status) {
        RenderStatus.LOADING -> LoadingScreen()
        RenderStatus.SUCCESS -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            viewModel.acceleration > 5.7f -> "You are too aggressive!"
                            viewModel.acceleration < -7.5f -> "You are stopping very fast!"
                            else -> "Nice driving!"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(8.dp)
                            .weight(1.0f)
                    )

                    StyleIndicator(viewModel.aggressiveScores.last(), viewModel.comfortLevel)
                }

                Spacer(Modifier.height(16.dp))

                ScoreGraph(viewModel.modelProducer)

                Spacer(Modifier.height(16.dp))

                Button(onClick = { showDebug = true }) { Text("Show Debug Info") }

                if (showDebug) {
                    DebugInfo(viewModel) { showDebug = false }
                }
            }
        }
        RenderStatus.ERROR -> ErrorScreen()
    }
}

@Composable
fun DebugInfo(viewModel: OBDViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                LazyColumn {
                    items(viewModel.obdData.toList()) { (key, value) ->
                        Text(
                            "$key: ${value.last().value} ${value.last().unit}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Comfort Level: ${viewModel.comfortLevel}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aggressive Driving State: ${viewModel.drivingState}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}