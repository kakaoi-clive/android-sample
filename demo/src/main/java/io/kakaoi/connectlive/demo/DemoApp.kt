package io.kakaoi.connectlive.demo

import android.app.Application
import io.kakaoi.connectlive.ConnectLive

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConnectLive.init(this)
    }
}