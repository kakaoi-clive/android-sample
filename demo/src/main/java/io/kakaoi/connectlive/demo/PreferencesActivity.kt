package io.kakaoi.connectlive.demo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference

class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    class Content : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_service, rootKey)

            findPreference<TwoStatePreference>(getString(R.string.key_use_external))?.run {
                onCheckedExternal(isChecked)

                setOnPreferenceChangeListener { _, newValue ->
                    onCheckedExternal(newValue == true)
                    true
                }
            }
        }

        private fun onCheckedExternal(checked: Boolean) {
            findPreference<Preference>(getString(R.string.key_service_key))?.isEnabled = !checked
            findPreference<Preference>(getString(R.string.key_secret))?.isEnabled = !checked
            findPreference<Preference>(getString(R.string.key_token))?.isEnabled = checked
        }
    }
}