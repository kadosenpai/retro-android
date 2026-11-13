package com.retro.app

import android.app.Application

class RetroApp : Application() {
    lateinit var model: AppModel
        private set

    override fun onCreate() {
        super.onCreate()
        model = AppModel(this)
    }
}
