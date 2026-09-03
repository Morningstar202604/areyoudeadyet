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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import com.silema.app.engine.HealthReport
import com.silema.app.engine.RiskEngine
import com.silema.app.wear.R
import com.silema.app.wear.Screen
import com.silema.app.wear.data.WearStore
import com.silema.app.wear.theme.riskColor

@Composable
fun AiBriefScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit = {}) {
    val records by WearStore.records.collectAsState()
    val workouts by WearStore.workouts.collectAsState()
    val assessment = remember(records) { RiskEngine.evaluate(records) }
    val weekly = remember(records, workouts) {
        HealthReport.weekly(records, workouts, System.currentTimeMillis(), assessment.alerts.size)
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 32.dp)) {
        item {
            Text(
                text = stringResource(R.string.ai_title),
                style = MaterialTheme.typography.title1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (records.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_no_data),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            repeat(weekly.summary.size) { i ->
                item {
                    Text(
                        text = weekly.summary[i],
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Text(
                    text = "${stringResource(R.string.ai_risk_level)}: ${assessment.level.label}",
                    color = riskColor(assessment.level),
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item { Button(onClick = { onNavigate(Screen.Home) }) { Text(stringResource(R.string.common_back)) } }
    }
}
