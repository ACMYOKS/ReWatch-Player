package com.amoscyk.android.rewatchplayer.ui.setting

import android.os.Bundle
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity

class SettingsActivity : ReWatchPlayerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            setupView()
        }
    }

    private fun setupView() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.root_view, SettingsFragment())
            .commit()
    }
}
