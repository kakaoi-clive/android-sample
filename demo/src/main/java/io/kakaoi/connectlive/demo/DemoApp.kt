package io.kakaoi.connectlive.demo

import android.app.Application
import androidx.preference.PreferenceManager
import io.kakaoi.connectlive.ConnectLive
import io.kakaoi.connectlive.demo.util.Preferences

class DemoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConnectLive.init(this)

        PreferenceManager.setDefaultValues(this, R.xml.preferences_service, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_lobby, false)

        Preferences.default(this).let { prefs ->
            ConnectLive.forcedApiServer =
                prefs.getString(getString(R.string.key_api_url), null)
            ConnectLive.forcedTurnUrls =
                prefs.getString(getString(R.string.key_turn_server), null)?.split(',')
            ConnectLive.forcedTurnUsername =
                prefs.getString(getString(R.string.key_turn_username), null)
            ConnectLive.forcedTurnCredential =
                prefs.getString(getString(R.string.key_turn_credential), null)
        }
    }
}