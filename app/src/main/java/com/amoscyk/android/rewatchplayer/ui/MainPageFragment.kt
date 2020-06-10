package com.amoscyk.android.rewatchplayer.ui


import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.navigation.findNavController
import androidx.transition.AutoTransition
import androidx.transition.Slide
import androidx.transition.TransitionManager

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.ui.player.VideoPlayerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_main_page.view.*

class MainPageFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private val bottomNav by lazy { rootView!!.main_bottom_nav }
    private val playerViewPadding by lazy { rootView!!.player_view_padding }
    val contentContainer by lazy { rootView!!.content_container }

    private var hasActionMode = false
    private var currentPlayerSize = VideoPlayerLayout.PlayerSize.DISMISS

    private val playerSizeListener =  object : VideoPlayerLayout.PlayerSizeListener {
        override fun onStart(start: VideoPlayerLayout.PlayerSize, end: VideoPlayerLayout.PlayerSize) {

        }

        override fun onComplete(current: VideoPlayerLayout.PlayerSize) {
            currentPlayerSize = current
            updateViews()
        }
    }

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
        setupBottomNavigation(savedInstanceState == null)
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.addPlayerSizeListener(playerSizeListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainActivity?.removePlayerSizeListener(playerSizeListener)
    }

    fun onSupportActionModeStarted(actionMode: ActionMode?) {
        hasActionMode = true
        updateViews()
    }

    fun onSupportActionModeFinished(actionMode: ActionMode?) {
        hasActionMode = false
        updateViews()
    }

    private fun updateViews() {
        TransitionManager.beginDelayedTransition(rootView as ViewGroup, Slide(Gravity.BOTTOM).apply {
            addTarget(playerViewPadding)
            addTarget(bottomNav)
        })
        playerViewPadding.visibility =
            if (currentPlayerSize == VideoPlayerLayout.PlayerSize.SMALL) View.VISIBLE
            else View.GONE
        bottomNav.visibility =
            if (currentPlayerSize == VideoPlayerLayout.PlayerSize.FULLSCREEN || hasActionMode)
                View.GONE
            else View.VISIBLE
    }

    private fun setupBottomNavigation(requireAttach: Boolean) {
        bottomNav.setupWithNavController(
            listOf(
                R.navigation.home,
                R.navigation.library,
                R.navigation.downloads,
                R.navigation.settings),
            childFragmentManager,
            R.id.main_page_nav_host_fragment,
            requireAttach
        )
    }
}
