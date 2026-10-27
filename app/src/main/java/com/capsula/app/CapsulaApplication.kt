package com.capsula.app

import android.app.Application
import com.capsula.app.di.AppContainer

class CapsulaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
