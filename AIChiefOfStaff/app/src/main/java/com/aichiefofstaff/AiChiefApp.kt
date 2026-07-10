package com.aichiefofstaff

import android.app.Application

class AiChiefApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
