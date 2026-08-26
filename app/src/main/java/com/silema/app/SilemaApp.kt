package com.silema.app

import android.app.Application
import com.silema.app.store.AppRepository

class SilemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppRepository.init(this)
    }
}
