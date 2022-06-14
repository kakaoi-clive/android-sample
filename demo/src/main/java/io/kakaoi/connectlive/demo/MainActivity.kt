package io.kakaoi.connectlive.demo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.commit
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import io.kakaoi.connectlive.demo.databinding.ActivityMainBinding
import io.kakaoi.connectlive.demo.ui.conference.ConferenceFragment
import io.kakaoi.connectlive.demo.ui.lobby.LobbyFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.connect.setOnClickListener { view ->

        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        navView.setNavigationItemSelectedListener(::onNavigationItemSelected)

        if (savedInstanceState == null) {
            navView.menu.performIdentifierAction(R.id.nav_lobby, 0)
        }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_lobby -> supportFragmentManager.commit {
                replace(R.id.content, LobbyFragment())
            }
            R.id.nav_conference -> supportFragmentManager.commit {
                replace(R.id.content, ConferenceFragment())
            }
            else -> return false
        }

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

    override fun onBackPressed() {
        super.onBackPressed()
    }
}