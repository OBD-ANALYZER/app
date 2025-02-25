package com.eb.obd2.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun StyleIndicator(aggressiveScore: Float, comfortLevel: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (aggressiveScore > 0.8) MaterialTheme.colorScheme.error else Color.Green,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (comfortLevel < 1) MaterialTheme.colorScheme.error else Color.Transparent,
                    shape = CircleShape
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StyleIndicatorPreview() = StyleIndicator(0.0f, 0.0f)