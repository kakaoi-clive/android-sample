package io.kakaoi.connectlive.demo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import io.kakaoi.connectlive.demo.databinding.ActivityMainBinding
import io.kakaoi.connectlive.demo.databinding.NavHeaderMainBinding
import io.kakaoi.connectlive.demo.ui.ConferenceFragment
import io.kakaoi.connectlive.demo.ui.LobbyFragment
import io.kakaoi.connectlive.demo.util.Preferences
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val navView: NavigationView = binding.navView

        navView.addHeaderView(
            NavHeaderMainBinding.inflate(layoutInflater).apply {
                sdkVersion.text = getString(
                    R.string.sdk_version_is,
                    io.kakaoi.connectlive.BuildConfig.VERSION_NAME  // TODO use ConnectLive.VERSION
                )
            }.root
        )

        navView.setNavigationItemSelectedListener(::onNavigationItemSelected)

        binding.connect.setOnClickListener {
            val roomId: String?
            val cameraEnabled: Boolean
            val micEnabled: Boolean
            val preferFrontCamera: Boolean

            Preferences.default(this).also {
                roomId = it.getString(getString(R.string.key_room_id), null)?.trim()
                cameraEnabled = it.getBoolean(getString(R.string.key_camera_enabled), false)
                micEnabled = it.getBoolean(getString(R.string.key_mic_enabled), false)
                preferFrontCamera = it.getBoolean(getString(R.string.key_camera_front), true)
            }

            if (roomId.isNullOrEmpty())
                Snackbar.make(it, R.string.no_room_id, Snackbar.LENGTH_LONG).show()
            else ConferenceService.start(this, roomId, cameraEnabled, micEnabled, preferFrontCamera)
        }

        ConferenceService.state
            .onEach(::onServiceStateChanged)
            .launchIn(lifecycleScope)
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawerLayout.close()
        when (item.itemId) {
            R.id.action_disconnect -> ConferenceService.stop(this)
            R.id.action_preferences -> startActivity(Intent(this, PreferencesActivity::class.java))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> binding.drawerLayout.open()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onBackPressed() {
        when {
            binding.drawerLayout.isOpen -> binding.drawerLayout.close()
            ConferenceService.state.value != ConferenceService.State.DISCONNECTED ->
                AlertDialog.Builder(this)
                    .setMessage(R.string.message_stop_conference)
                    .setCancelable(true)
                    .setPositiveButton(R.string.stop) { _, _ ->
                        ConferenceService.stop(this)
                    }
                    .show()
            else -> super.onBackPressed()
        }
    }

    private fun onServiceStateChanged(state: ConferenceService.State) {
        supportFragmentManager.run {
            val fragmentClass = when (state) {
                ConferenceService.State.CONNECTED -> ConferenceFragment::class.java
                else -> LobbyFragment::class.java
            }

            if (findFragmentById(R.id.content)?.javaClass != fragmentClass) {
                commit { replace(R.id.content, fragmentClass.newInstance()) }
            }
        }

        binding.connect.isVisible = state == ConferenceService.State.DISCONNECTED

        binding.connecting.isVisible = state is ConferenceService.State.CONNECTING

        if (state is ConferenceService.State.CONNECTING) {
            binding.connectingProgress.progress = (state.progress * 100).toInt()
        }

        binding.navView.menu.findItem(R.id.action_preferences).isEnabled =
            state == ConferenceService.State.DISCONNECTED

        binding.navView.menu.findItem(R.id.action_disconnect).isEnabled =
            state == ConferenceService.State.CONNECTED
    }
}