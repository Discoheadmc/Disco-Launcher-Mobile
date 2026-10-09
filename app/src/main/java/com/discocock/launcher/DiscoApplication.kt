package com.discocock.launcher

import android.app.Application
import com.discocock.launcher.di.AppContainer

class DiscoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
