package com.silema.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.silema.app.ui.theme.SilemaTheme

@Preview(name = "Report V3", showBackground = true)
@Composable
private fun PreviewReport() {
    SilemaTheme {
        ReportScreenV3(records = emptyList())
    }
}

@Preview(name = "Family V3", showBackground = true)
@Composable
private fun PreviewFamily() {
    SilemaTheme {
        FamilyScreenV3()
    }
}

@Preview(name = "Guardian V3", showBackground = true)
@Composable
private fun PreviewGuardian() {
    SilemaTheme {
        GuardianScreenV3(contacts = emptyList())
    }
}

@Preview(name = "Medical V3", showBackground = true)
@Composable
private fun PreviewMedical() {
    SilemaTheme {
        MedicalScreenV3()
    }
}

@Preview(name = "Devices V3", showBackground = true)
@Composable
private fun PreviewDevices() {
    SilemaTheme {
        DevicesScreenV3()
    }
}

@Preview(name = "AI Report V3", showBackground = true)
@Composable
private fun PreviewAiReport() {
    SilemaTheme {
        AiReportScreenV3(records = emptyList())
    }
}

@Preview(name = "Workout V3", showBackground = true)
@Composable
private fun PreviewWorkout() {
    SilemaTheme {
        WorkoutScreenV3()
    }
}

@Preview(name = "SOS V3", showBackground = true)
@Composable
private fun PreviewSos() {
    SilemaTheme {
        SosScreenV3()
    }
}
