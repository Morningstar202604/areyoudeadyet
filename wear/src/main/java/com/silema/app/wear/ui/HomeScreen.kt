package com.silema.app.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.silema.app.engine.RiskEngine
import com.silema.app.wear.R
import com.silema.app.wear.Screen
import com.silema.app.wear.data.WearStore
import com.silema.app.wear.theme.riskColor

@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit = {}) {
    val records by WearStore.records.collectAsState()
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val hasData = records.isNotEmpty()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 32.dp)) {
        item {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.title1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = if (hasData) assessment.level.label else stringResource(R.string.home_no_data),
                color = if (hasData) riskColor(assessment.level) else MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.display1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            val statusText = if (hasData) {
                if (assessment.alerts.isNotEmpty()) {
                    stringResource(R.string.home_alerts_count, assessment.alerts.size)
                } else {
                    stringResource(R.string.home_all_normal)
                }
            } else {
                stringResource(R.string.home_tap_to_enter)
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val recent = records.takeLast(5)
        repeat(recent.size) { i ->
            val rec = recent[i]
            item {
                Chip(
                    onClick = { onNavigate(Screen.Entry) },
                    label = { Text(rec.type?.displayName ?: rec.typeId) },
                    secondaryLabel = { Text("${rec.value.toInt()} ${rec.type?.unit ?: ""}") }
                )
            }
        }

        item { Button(onClick = { onNavigate(Screen.Entry) }) { Text(stringResource(R.string.entry_title)) } }
        item { Button(onClick = { onNavigate(Screen.Sos) }) { Text(stringResource(R.string.sos_title)) } }
        item { Button(onClick = { onNavigate(Screen.Workout) }) { Text(stringResource(R.string.workout_title)) } }
        item { Button(onClick = { onNavigate(Screen.Ai) }) { Text(stringResource(R.string.ai_title)) } }
    }
}
