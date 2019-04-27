package dev.entao.utilapp

import android.app.Application
import dev.entao.kan.appbase.App

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        App.init(this)
    }
}