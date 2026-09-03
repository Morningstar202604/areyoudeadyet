package com.silema.app.wear.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.wear.R
import com.silema.app.wear.Screen
import com.silema.app.wear.data.WearStore

@Composable
fun EntryScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit = {}) {
    val types = listOf(
        VitalType.HEART_RATE, VitalType.SYSTOLIC, VitalType.DIASTOLIC,
        VitalType.SPO2, VitalType.TEMPERATURE, VitalType.STEPS
    )
    var typeIndex by remember { mutableStateOf(0) }
    val type = types[typeIndex]

    val range = when (type) {
        VitalType.HEART_RATE -> 40..200
        VitalType.SYSTOLIC -> 70..220
        VitalType.DIASTOLIC -> 40..140
        VitalType.SPO2 -> 70..100
        VitalType.TEMPERATURE -> 35..42
        VitalType.STEPS -> 0..30000
        else -> 0..300
    }

    var selected by remember { mutableStateOf(range.first + (range.last - range.first) / 2) }
    LaunchedEffect(typeIndex) { selected = range.first + (range.last - range.first) / 2 }

    val pickerState = rememberPickerState(
        initialNumberOfOptions = range.last - range.first + 1,
        initiallySelectedOption = selected - range.first
    )

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 32.dp)) {
        item {
            Text(
                text = stringResource(R.string.entry_title),
                style = MaterialTheme.typography.title1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Chip(
                onClick = { typeIndex = (typeIndex + 1) % types.size },
                label = { Text(type.displayName) },
                secondaryLabel = { Text(stringResource(android.R.string.ok)) }
            )
        }
        item {
            Picker(
                state = pickerState,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = type.displayName
            ) { opt ->
                Text(text = (range.first + opt).toString(), style = MaterialTheme.typography.display1)
            }
        }
        item {
            Button(onClick = {
                val v = (range.first + pickerState.selectedOption).toDouble()
                WearStore.addRecord(
                    VitalRecord.of(type, v, System.currentTimeMillis(), VitalSource.MANUAL)
                )
                onNavigate(Screen.Home)
            }) { Text(stringResource(R.string.common_confirm)) }
        }
    }
}
