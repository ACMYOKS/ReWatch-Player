package com.amoscyk.android.rewatchplayer.ui


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainPageFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_main_page, container, false)
        }
        return rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        rootView?.let { rootView ->
            val bottomNav = rootView.findViewById<BottomNavigationView>(R.id.main_bottom_nav)
            bottomNav.setupWithNavController(
                listOf(
                    R.navigation.home,
                    R.navigation.library,
                    R.navigation.downloads,
                    R.navigation.settings),
                childFragmentManager,
                R.id.main_page_nav_host_fragment
            )
        }
    }
}
