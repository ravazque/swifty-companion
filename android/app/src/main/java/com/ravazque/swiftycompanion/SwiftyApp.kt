package com.ravazque.swiftycompanion

import android.app.Application

class SwiftyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
