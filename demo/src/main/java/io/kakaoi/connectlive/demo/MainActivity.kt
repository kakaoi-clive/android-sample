package io.kakaoi.connectlive.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import io.kakaoi.connectlive.demo.databinding.ActivityMainBinding
import io.kakaoi.connectlive.demo.databinding.NavHeaderMainBinding
import io.kakaoi.connectlive.demo.ui.conference.ConferenceFragment
import io.kakaoi.connectlive.demo.ui.lobby.LobbyFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

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
            ConferenceService.start(this)
        }

        binding.disconnect.setOnClickListener {
            ConferenceService.stop(this)
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
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> binding.drawerLayout.open()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onBackPressed() = when {
        binding.drawerLayout.isOpen -> binding.drawerLayout.close()
        // TODO disconnect if connected
        else -> super.onBackPressed()
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

        binding.disconnect.isVisible = state == ConferenceService.State.CONNECTED

        binding.connecting.isVisible = state is ConferenceService.State.CONNECTING

        if (state is ConferenceService.State.CONNECTING) {
            binding.connectingProgress.progress = (state.progress * 100).toInt()
        }
    }
}