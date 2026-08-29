package com.silema.app.wear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import com.silema.app.data.VitalType
import com.silema.app.data.Workout
import com.silema.app.wear.R
import com.silema.app.wear.Screen
import com.silema.app.wear.data.WearStore
import java.util.UUID

@Composable
fun WorkoutScreen(onNavigate: (Screen) -> Unit) {
    val records by WearStore.records.collectAsState()
    val workouts by WearStore.workouts.collectAsState()

    val steps = records
        .filter { it.typeId == VitalType.STEPS.id }
        .maxByOrNull { it.timestampMillis }?.value?.toInt() ?: 0

    var isRun by remember { mutableStateOf(false) }
    val durationState = rememberPickerState(
        initialNumberOfOptions = 11,
        initiallySelectedOption = 2
    )

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = stringResource(R.string.workout_title),
                style = MaterialTheme.typography.title1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = "${stringResource(R.string.workout_today_steps)} $steps",
                style = MaterialTheme.typography.display1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = "${stringResource(R.string.workout_weekly_count)} ${workouts.size}",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Chip(
                onClick = { isRun = !isRun },
                label = { Text(if (isRun) "Run" else "Walk") },
                secondaryLabel = { Text("Tap to switch") }
            )
        }
        item {
            Picker(
                state = durationState,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = stringResource(R.string.workout_duration)
            ) { opt ->
                Text("${10 + opt * 5} min", style = MaterialTheme.typography.body1)
            }
        }
        item {
            Button(onClick = {
                val durationMin = 10 + durationState.selectedOption * 5
                WearStore.addWorkout(
                    Workout(
                        id = UUID.randomUUID().toString(),
                        type = if (isRun) "run" else "walk",
                        startMillis = System.currentTimeMillis(),
                        durationMillis = durationMin * 60_000L,
                        distanceMeters = 0.0,
                        caloriesKcal = 0.0,
                        track = emptyList()
                    )
                )
                onNavigate(Screen.Home)
            }) { Text(stringResource(R.string.workout_save)) }
        }
    }
}
