package com.amoscyk.android.rewatchplayer.ui


import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.transition.Slide
import androidx.transition.TransitionManager

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.ui.player.VideoPlayerLayout
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getInt
import com.amoscyk.android.rewatchplayer.util.putInt
import kotlinx.android.synthetic.main.fragment_main_page.view.*

class MainPageFragment : ReWatchPlayerFragment() {

    private val bottomNav by lazy { view!!.main_bottom_nav }
    private val playerViewPadding by lazy { view!!.player_view_padding }
    val contentContainer by lazy { view!!.content_container }

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
        return inflater.inflate(R.layout.fragment_main_page, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation(savedInstanceState == null)
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.addPlayerSizeListener(playerSizeListener)

        // if last open tab is not set, use default tab
        bottomNav.selectedItemId =
            activity!!.appSharedPreference.getInt(PreferenceKey.LAST_OPEN_TAB_ID, -1)
    }

    override fun onStop() {
        super.onStop()
        mainActivity?.removePlayerSizeListener(playerSizeListener)
        activity!!.appSharedPreference.edit()
            .putInt(PreferenceKey.LAST_OPEN_TAB_ID, bottomNav.selectedItemId)
            .apply()
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
        TransitionManager.beginDelayedTransition(view as ViewGroup, Slide(Gravity.BOTTOM).apply {
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
