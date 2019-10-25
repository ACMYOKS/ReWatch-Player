package com.amoscyk.android.rewatchplayer.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.amoscyk.android.rewatchplayer.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.main_bottom_nav)
        bottomNav.setupWithNavController(
            listOf(
                R.navigation.home,
                R.navigation.library,
                R.navigation.downloads,
                R.navigation.account),
            supportFragmentManager,
            R.id.main_nav_host_fragment
        )
    }
}
