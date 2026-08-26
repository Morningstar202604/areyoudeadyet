package com.silema.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.silema.app.store.AppRepository
import com.silema.app.ui.AppRoot
import com.silema.app.ui.theme.SilemaTheme
import com.silema.app.util.TtsController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilemaTheme {
                val context = LocalContext.current
                val tts = remember { TtsController(context) }
                DisposableEffect(Unit) {
                    onDispose { tts.shutdown() }
                }
                val records by AppRepository.records.collectAsState()
                val contacts by AppRepository.contacts.collectAsState()
                AppRoot(records = records, contacts = contacts, tts = tts)
            }
        }
    }
}
