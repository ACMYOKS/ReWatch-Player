package com.amoscyk.android.rewatchplayer.ui


import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.*

import com.amoscyk.android.rewatchplayer.util.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main_page2.view.*

class MainPageFragment : ReWatchPlayerFragment() {

    enum class PlayerSize {
        DISMISS, SMALL, FULLSCREEN
    }

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val motionLayout get() = view!! as InterveneTransitionMotionLayout
    private val playerToolbar get() = view!!.player_toolbar
    private val tvTitle get() = view!!.tv_video_title
    private val bottomNav get() = view!!.main_bottom_nav
    val contentContainer get() = view!!.content_container
    private val playerHolder get() = view!!.player_holder
    private val playerView get() = view!!.player_view
    private val tvTitleSmall get() = view!!.tv_video_title_small
    private val btnPlayback get() = view!!.btn_playback_small
    private val btnClose get() = view!!.btn_close_small
    val playerControl get() = view!!.player_control

    private var isBookmarked: Boolean = false
    private var hasPendingUiUpdate: Boolean = false

    private var actionMode: ActionMode? = null

    val isSmall get() = motionLayout.currentState == R.id.small
    val isFullscreen get() = motionLayout.currentState == R.id.fullscreen
    val isDimiss get() = motionLayout.currentState == R.id.dismiss

    private var pendingNavIndex: Int? = null
    private var isStarted = false

    private val playerListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            btnPlayback.setImageResource(
                if (isPlaying) R.drawable.ic_pause_white
                else R.drawable.ic_play_arrow_white
            )
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        object : MediatorLiveData<Boolean>() {
            var vid = listOf<String>()
            var currentVid: String? = null

            init {
                addSource(mainViewModel.bookmarkedVid) {
                    vid = it
                    value = currentVid in vid
                }
                addSource(mainViewModel.videoData) {
                    currentVid = it.videoMeta.videoId
                    value = currentVid!! in vid
                }
            }
        }.observe(this, Observer { isBookmarked ->
            setBookmarked(isBookmarked)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_page2, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNavigation(savedInstanceState == null)
        setupView()
        doPendingUiUpdate()
        Handler().postDelayed({
            motionLayout.setStateAndDisableOtherTransitions(R.id.dismiss)
        }, 0L)

    }

    override fun onStart() {
        super.onStart()
        // check if there is explicitly requested nav page to open, if not exists, check if last
        // open tab is not set, use default tab
        isStarted = true
        pendingNavIndex?.let { navigateToPage(it) } ?: run {
            bottomNav.selectedItemId =
                activity!!.appSharedPreference.getInt(PreferenceKey.LAST_OPEN_TAB_ID, -1)
        }
    }

    override fun onStop() {
        super.onStop()
        activity!!.appSharedPreference.edit()
            .putInt(PreferenceKey.LAST_OPEN_TAB_ID, bottomNav.selectedItemId)
            .apply()
        isStarted = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rpApplication.getPlayer()?.removeListener(playerListener)
        playerView.player = null
        playerControl.player = null
    }

    fun onSupportActionModeStarted(actionMode: ActionMode?) {
        this.actionMode = actionMode
    }

    fun onSupportActionModeFinished(actionMode: ActionMode?) {
        this.actionMode = null
    }

    private fun setupView() {
        motionLayout.apply {
            setTransitionListener(object : MotionLayout.TransitionListener {
                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float
                ) {

                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int
                ) {
                    playerControl.hide()
                    if (endId == R.id.fullscreen) {
                        activity?.requestedOrientation =
                            when (resources.configuration.orientation) {
                                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                    }
                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) {
                    if (startId == R.id.fullscreen && endId == R.id.small && progress > 0.9) {
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    }
                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    when (currentId) {
                        R.id.fullscreen -> {
                            Log.d(
                                AppConstant.TAG,
                                "transition completed, is fullscreen, show control view"
                            )
                            // sync visibility setting of player control view with motion layout
                            Handler().postDelayed({ playerControl.show() }, 0)
                            val orientation = resources.configuration.orientation
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) requireActivity().hideSystemUI()
                            else requireActivity().showSystemUI()
                            playerControl.show()
                        }
                        R.id.small -> {
                        }
                        R.id.dismiss -> {
                            Log.d(AppConstant.TAG, "transition completed, is dismiss, stop video")
                            playerView.player.stop()
                            requireActivity().apply {
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                showSystemUI()
                            }
                        }
                    }
                }
            })
            addIgnoreTransitionView(playerControl)
            addIgnoreTransitionView(playerToolbar)
            registerViewForOnClickEvent(playerView) {
                showControlView()
            }
            registerViewForOnClickEvent(playerHolder) {
                showControlView()
            }
            registerViewForOnClickEvent(btnClose) {
                motionLayout.transitionToState(R.id.dismiss)
            }
            registerViewForOnClickEvent(btnPlayback) {
                playerView.player.apply { playWhenReady = !isPlaying }      // toggle play state
            }
        }
        val mainPlayer = rpApplication.getPlayer()?.apply {
            addListener(playerListener)
        }
        playerView.apply {
            player = mainPlayer
            useController = false
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        playerControl.apply {
            player = mainPlayer
            setVisibilityListener { visibility ->
                playerToolbar.visibility = visibility
            }
        }
        playerToolbar.apply {
            inflateMenu(R.menu.player_option_menu)
            setMenuItemTintColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.bookmark -> {
                        mainViewModel.videoData.value?.videoMeta?.videoId?.let { vid ->
                            mainViewModel.setBookmarked(vid, true)
                        }
                    }
                    R.id.remove_bookmark -> {
                        mainViewModel.videoData.value?.videoMeta?.videoId?.let { vid ->
                            mainViewModel.setBookmarked(vid, false)
                        }
                    }
                    R.id.rotation -> {
                        requireActivity().toggleLockedRotation()
                    }
                    R.id.other_action -> {
                        mainActivity?.getPlayerOptionDialog()?.show()
                    }
                }
                true
            }
            setBookmarked(false)
        }
        playerControl.hide()
        tvTitle.isSelected = true           // enable marquee
        tvTitleSmall.isSelected = true      // enable marquee
    }

    fun setTitle(title: String) {
        tvTitle.text = title
        tvTitleSmall.text = title
    }

    fun setPlayerSize(size: PlayerSize) {
        motionLayout.transitionToState(
            when (size) {
                PlayerSize.DISMISS -> R.id.dismiss
                PlayerSize.SMALL -> R.id.small
                PlayerSize.FULLSCREEN -> R.id.fullscreen
            }
        )
    }

    fun showControlView() {
        if (motionLayout.currentState == R.id.fullscreen) {
            playerControl.show()
        }
    }

    fun hideControlView() {
        playerControl.hide()
    }

    fun navigateToPage(pageIndex: Int) {
        val menu = bottomNav.menu
        if (pageIndex in 0..menu.size()) {
            pendingNavIndex = pageIndex
            if (isStarted) {
                bottomNav.selectedItemId = menu.getItem(pageIndex).itemId
                pendingNavIndex = null
            }
        }
    }

    private fun getViewForSnackbar(): View = if (motionLayout.currentState == R.id.fullscreen) {
        playerHolder
    } else {
        contentContainer
    }

    fun showSnackbar(resId: Int, duration: Int) {
        newSnackbar(resId, duration).show()
    }

    fun showSnackbar(text: CharSequence, duration: Int) {
        newSnackbar(text, duration).show()
    }

    fun newSnackbar(resId: Int, duration: Int) =
        Snackbar.make(getViewForSnackbar(), resId, duration)

    fun newSnackbar(text: CharSequence, duration: Int) =
        Snackbar.make(getViewForSnackbar(), text, duration)


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (isSmall) {
                    setPlayerSize(PlayerSize.FULLSCREEN)
                }
                if (isFullscreen) activity?.hideSystemUI()
                else activity?.showSystemUI()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                activity?.showSystemUI()
            }
        }
    }

    private fun setBookmarked(isBookmarked: Boolean) {
        this.isBookmarked = isBookmarked
        if (view == null) hasPendingUiUpdate = true
        else doPendingUiUpdate()
    }

    private fun doPendingUiUpdate() {
        playerToolbar.menu.findItem(R.id.bookmark).isVisible = !isBookmarked
        playerToolbar.menu.findItem(R.id.remove_bookmark).isVisible = isBookmarked

        hasPendingUiUpdate = false
    }

    private fun setupBottomNavigation(requireAttach: Boolean) {
        bottomNav.setupWithNavController(
            listOf(
                R.navigation.library,
                R.navigation.downloads,
                R.navigation.settings
            ),
            childFragmentManager,
            R.id.main_page_nav_host_fragment,
            requireAttach
        ) {
            actionMode?.finish()
        }
    }
}
