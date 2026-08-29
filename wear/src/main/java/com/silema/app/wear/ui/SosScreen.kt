package com.silema.app.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.silema.app.wear.R
import com.silema.app.wear.Screen

@Composable
fun SosScreen(onNavigate: (Screen) -> Unit) {
    var triggered by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!triggered) {
            Button(
                onClick = { triggered = true },
                modifier = Modifier.size(120.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC62828))
            ) {
                Text(stringResource(R.string.sos_button), textAlign = TextAlign.Center, style = MaterialTheme.typography.button)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.sos_confirmed), color = Color(0xFFC62828), style = MaterialTheme.typography.display1)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sos_notifying), style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onNavigate(Screen.Home) }) { Text(stringResource(R.string.common_back)) }
            }
        }
    }
}
