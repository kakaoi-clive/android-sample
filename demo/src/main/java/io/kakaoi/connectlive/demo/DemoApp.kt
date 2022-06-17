package io.kakaoi.connectlive.demo

import android.app.Application
import androidx.preference.PreferenceManager
import io.kakaoi.connectlive.ConnectLive

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(this, R.xml.preferences_service, false)
        ConnectLive.init(this)
    }
}