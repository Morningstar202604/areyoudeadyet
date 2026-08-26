package com.silema.app

import android.app.Application
import com.silema.app.store.AppRepository
import com.silema.app.work.Reminders

class SilemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.init(this)
        Reminders.ensureChannel(this)
    }
}
