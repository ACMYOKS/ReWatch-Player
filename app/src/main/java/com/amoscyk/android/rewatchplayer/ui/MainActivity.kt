package com.amoscyk.android.rewatchplayer.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.amoscyk.android.rewatchplayer.*
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.service.AudioPlayerService
import com.amoscyk.android.rewatchplayer.ui.MainViewModel.ResponseActionType
import com.amoscyk.android.rewatchplayer.ui.library.LibraryFragmentDirections
import com.amoscyk.android.rewatchplayer.ui.player.*
import com.amoscyk.android.rewatchplayer.util.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import java.io.File


class MainActivity : ReWatchPlayerActivity() {

    private val mPlayerLayout by lazy { findViewById<VideoPlayerLayout>(R.id.video_player_layout) }

    private var mLoadingDialog: AlertDialog? = null

    // player option view
    private var mOptionDialog: BottomSheetDialog? = null
    private var mVideoInfoDialog: VideoInfoBottomSheetDialog? = null
    private var mArchiveOptionDialog: ArchiveOptionDialog? = null
    private var mResOptionDialog: VideoQualityOptionDialog? = null
    private var mPlaybackSpeedDialog: PlaybackSpeedDialog? = null

    // player option view related class
    private val mOptionAdapter = PlayerOptionAdapter({ option, position ->
        mOptionDialog?.dismiss()
        if (position == 0) {
            mVideoInfoDialog?.show()
        } else if (position == 1) {
            mResOptionDialog?.show()
        } else if (position == 2) {
            viewModel.videoData.value?.videoMeta!!.videoMeta.videoId.let { vid ->
                showArchiveOption(vid)
            }
        } else if (position == 3) {
            viewModel.playbackParams.value?.speed?.let { speed ->
                mPlaybackSpeedDialog?.apply {
                    setPlaybackMultiplier(speed)
                    show()
                }
            }
        } else if (position == 4) {
            Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" +
                        viewModel.videoData.value!!.videoMeta.videoMeta.videoId)
                type = "text/plain"
            }, getString(R.string.player_share_title)).also { startActivity(it) }
        }
    })

    private val extraPlayerOptions by lazy {
        listOf(
            PlayerOption(getString(R.string.player_option_info), R.drawable.ic_info),
            PlayerOption(getString(R.string.player_option_resolution), R.mipmap.ic_action_resolution),
            PlayerOption(getString(R.string.player_option_archive), R.drawable.ic_archive_white),
            PlayerOption(getString(R.string.player_option_speed), R.drawable.ic_slow_motion_video),
            PlayerOption(getString(R.string.player_option_share), R.drawable.ic_share)
        )
    }

    private val viewModel by viewModels<MainViewModel> { viewModelFactory }

    private var isWifiConnected = false
    private var isMobileDataConnected = false
    private var allowPlayUsingMobile = false
    private var allowDownloadUsingMobile = false

    private var exoPlayer: SimpleExoPlayer? = null
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0
    private val defaultFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(this, getString(R.string.app_name))
    }
    private val progressiveSrcFactory: ProgressiveMediaSource.Factory by lazy {
        ProgressiveMediaSource.Factory(defaultFactory)
    }

    private val mPlayerListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                exoPlayer?.apply {
                    viewModel.saveWatchHistory(currentPosition)
                }
            }
        }
    }
    private var mPlayerServiceReceiver: BroadcastReceiver? = null

    private var mPlayerSizeListeners = ArrayList<VideoPlayerLayout.PlayerSizeListener>()

    private var mPipActionReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // init account name for youtube repo every time main activity is created
        // to prevent null account name on app restart
        appSharedPreference.getString(PreferenceKey.ACCOUNT_NAME, null)?.let {
            viewModel.setAccountName(it)
        }

        initPlayer()
        setupViews()

        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        ConnectivityLiveData(connMgr, ConnectivityLiveData.TransportType.WIFI).apply {
            observe(this@MainActivity, Observer {
                isWifiConnected = it == ConnectivityLiveData.ConnectivityStatus.CONNECTED
                viewModel.notifyIsWifiConnected(isWifiConnected)
                Log.d(AppConstant.TAG, "$TAG wifi connected: ${it.name}")
            })
        }
        ConnectivityLiveData(connMgr, ConnectivityLiveData.TransportType.MOBILE).apply {
            observe(this@MainActivity, Observer {
                isMobileDataConnected = it == ConnectivityLiveData.ConnectivityStatus.CONNECTED
                viewModel.notifyIsMobileDataConnected(isMobileDataConnected)
                Log.d(AppConstant.TAG, "$TAG mobile data connected: ${it.name}")
            })
        }
        SPIntLiveData(appSharedPreference,
            PreferenceKey.ALLOW_VIDEO_STREAMING_ENV,
            AppSettings.DEFAULT_ALLOW_VIDEO_STREAMING_ENV).apply {
            observe(this@MainActivity, Observer {
                allowPlayUsingMobile = it == 1
                viewModel.notifyIsAllowedPlayUsingMobile(allowPlayUsingMobile)
            })
        }
        SPIntLiveData(appSharedPreference,
            PreferenceKey.PLAYER_SKIP_FORWARD_TIME,
            AppSettings.DEFAULT_PLAYER_SKIP_FORWARD_SECOND).apply {
            observe(this@MainActivity, Observer {
                mPlayerLayout.playerControlView.setFastForwardIncrementMs(it * 1000)
            })
        }
        SPIntLiveData(appSharedPreference,
            PreferenceKey.PLAYER_SKIP_BACKWARD_TIME,
            AppSettings.DEFAULT_PLAYER_SKIP_BACKWARD_SECOND).apply {
            observe(this@MainActivity, Observer {
                mPlayerLayout.playerControlView.setRewindIncrementMs(it * 1000)
            })
        }
        SPIntLiveData(appSharedPreference,
            PreferenceKey.ALLOW_DOWNLOAD_ENV,
            AppSettings.DEFAULT_ALLOW_DOWNLOAD_ENV).apply {
            observe(this@MainActivity, Observer {
                allowDownloadUsingMobile = it == 1
                viewModel.notifyIsAllowedDownloadUsingMobile(allowDownloadUsingMobile)
            })
        }

        viewModel.needShowArchiveOption.observe(this, Observer { event ->
            event.getContentIfNotHandled { mArchiveOptionDialog?.show() }
        })

//        viewModel.bookmarkToggled.observe(this, Observer {
//            viewModel.videoMeta.value?.apply {
//                bookmarked = !bookmarked
//                setBookmarkButton(bookmarked)
//            }
//            viewModel.videoData.value?.videoMeta?.videoMeta?.apply {
//                bookmarked = !bookmarked
//                setBookmarkButton(bookmarked)
//            }
//        })

        viewModel.searchVideoResource.observe(this, Observer { res ->
            when (res.status) {
                Status.LOADING -> {
                    Toast.makeText(this, "Checking...", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    viewModel.playVideoForId(this, res.data!!,
                        appSharedPreference.getBoolean(
                            PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST,
                            AppSettings.DEFAULT_PLAYER_PLAY_DOWNLOADED_IF_EXIST
                        )
                    )
                }
                Status.ERROR -> {
                    Toast.makeText(
                        this,
                        "video with id ${res.data!!} not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

//        viewModel.videoMeta.observe(this, Observer { meta ->
//            mPlayerLayout.setTitle(meta.title)
//            setBookmarkButton(meta.bookmarked)
//            val itags = meta.itags
//            val vTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
//                itags.contains(it.key)
//            })
//            val aTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
//                itags.contains(it.key)
//            })
//            mResOptionDialog?.apply {
//                setVideoTags(vTags.keys.toList())
//                setAudioVTags(aTags.keys.toList())
//            }
//            mVideoInfoDialog?.apply {
//                setVideoMeta(meta)
//                setViewChannelButtonOnClickListener(View.OnClickListener {
//                    dismiss()
//                    findNavController(R.id.main_page_nav_host_fragment)
//                        .navigate(LibraryFragmentDirections.showChannelDetail(meta.channelId))
//                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                        handleRotation()
//                    }
//                    mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.SMALL)
//                })
//            }
//        })
        viewModel.bookmarkedVid.observe(this, Observer {

        })

        object : MediatorLiveData<Boolean>() {
            var vid = listOf<String>()
            var currentVid: String? = null
            init {
                addSource(viewModel.bookmarkedVid) {
                    vid = it
                    value = currentVid in vid
                }
                addSource(viewModel.videoData) {
                    currentVid = it.videoMeta.videoMeta.videoId
                    value = currentVid!! in vid
                }
            }
        }.observe(this, Observer { isBookmarked ->
            setBookmarkButton(isBookmarked)
        })

        viewModel.videoData.observe(this, Observer { viewData ->
            val meta = viewData.videoMeta.videoMeta
            mPlayerLayout.setTitle(meta.title)
            val itags = meta.itags
            val vTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
                itags.contains(it.key)
            })
            val aTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
                itags.contains(it.key)
            })
            mResOptionDialog?.apply {
                setVideoTags(vTags.keys.toList())
                setAudioVTags(aTags.keys.toList())
            }
            mVideoInfoDialog?.apply {
                setVideoMeta(meta)
                setViewChannelButtonOnClickListener(View.OnClickListener {
                    dismiss()
                    findNavController(R.id.main_page_nav_host_fragment)
                        .navigate(LibraryFragmentDirections.showChannelDetail(meta.channelId))
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        handleRotation()
                    }
                    mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.SMALL)
                })
            }
        })

        viewModel.archiveVideoMeta.observe(this, Observer { meta ->
            val itags = meta.itags
            val vTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
                itags.contains(it.key)
            })
            val aTags = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
                itags.contains(it.key)
            })
            mArchiveOptionDialog?.apply {
                setVideoTags(vTags.keys.toList())
                setAudioVTags(aTags.keys.toList())
            }
        })

        viewModel.selectedTags.observe(this, Observer {
            mResOptionDialog?.apply {
                it.vTag?.let { setCurrentVTag(it) }
                it.aTag?.let { setCurrentATag(it) }
            }
        })

        viewModel.resourceUri.observe(this, Observer {
            showPlayerView()
            prepareMediaResource(*it.uriList.toTypedArray())
            if (it.lastPlayPos != null) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.player_restore_last_play_position_message)
                    .setPositiveButton(R.string.confirm_text) { d, i ->
                        exoPlayer?.apply {
                            playWhenReady = true
                            seekTo(currentWindow, it.lastPlayPos)
                        }
                    }
                    .setNegativeButton(R.string.negative_text) { d, i ->
                        d.cancel()
                    }
                    .setOnCancelListener {
                        exoPlayer?.apply {
                            playWhenReady = true
                            seekTo(currentWindow, 0)
                        }
                    }
                    .create()
                    .show()
            } else {
                exoPlayer?.apply {
                    playWhenReady = true
                    seekTo(currentWindow, playbackPosition)
                }
            }
        })

        viewModel.shouldSaveWatchHistory.observe(this, Observer { event ->
            event.getContentIfNotHandled { videoId ->
                exoPlayer?.let {
                    Log.d("MainActivity", "save watch history, ${videoId}, ${it.currentPosition}")
                    viewModel.saveWatchHistory(videoId, it.currentPosition)
                }
            }
        })

        viewModel.archiveResult.observe(this, Observer { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    getMainFragment()?.contentContainer?.let {
                        Snackbar.make(
                            it,
                            result.data!!.taskCount.let {
                                if (it > 0) {
                                    getString(R.string.player_archive_new_tasks).format(it)
                                } else {
                                    getString(R.string.player_archive_no_new_tasks)
                                }
                            },
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                Status.ERROR -> {
                    getMainFragment()?.contentContainer?.let {
                        Snackbar.make(
                            it,
                            result.stringMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                else -> {
                }
            }
        })

        viewModel.isLoadingVideo.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (it) {
                    mLoadingDialog?.show()
                } else {
                    mLoadingDialog?.dismiss()
                }
            }
        })

        viewModel.shouldStartPlayVideo.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                exoPlayer?.let {
                    it.playWhenReady = true
                    it.seekTo(currentWindow, playbackPosition)
                }
            }
        })

        viewModel.shouldStopVideo.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                exoPlayer?.playWhenReady = false
            }
        })

        viewModel.getVideoResult.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                it.error?.let {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.player_error_title)
                        .setMessage(
                            when (it) {
                                MainViewModel.GetVideoResult.Error.EMPTY_VIDEO_ID -> R.string.player_error_no_video_id
                                MainViewModel.GetVideoResult.Error.NO_VIDEO_META -> R.string.player_error_no_video_meta
                                MainViewModel.GetVideoResult.Error.NO_YT_INFO -> R.string.player_error_no_yt_info
                                MainViewModel.GetVideoResult.Error.NO_QUALITY -> R.string.player_error_no_available_quality
                                MainViewModel.GetVideoResult.Error.UNKNOWN -> R.string.player_error_unknown
                            }
                        )
                        .create()
                        .show()
                }
            }
        })

        viewModel.playbackParams.observe(this, Observer {
            exoPlayer?.playbackParameters = it
        })

//        viewModel.videoResSelection.observe(this, Observer {
//            mResSelectionAdapter.apply { submitList(it) }
//        })
//
//        viewModel.audioQualitySelection.observe(this, Observer {
//            mAudioSelectionAdapter.apply { submitList(it) }
//        })

        viewModel.responseAction.observe(this, Observer { res ->
            when (res.status) {
                Status.LOADING -> {

                }
                Status.SUCCESS -> {
                    when (res.data) {
                        ResponseActionType.SHOW_ENABLE_MOBILE_DATA_USAGE_ALERT -> {
                            AlertDialog.Builder(this)
                                .setMessage(getString(R.string.player_alert_enable_stream_with_mobile_data))
                                .setPositiveButton(R.string.confirm_text) { _, _ ->
                                    appSharedPreference.edit(true) {
                                        putInt(PreferenceKey.ALLOW_VIDEO_STREAMING_ENV, 1)
                                    }
                                    viewModel.continueSetQuality()
                                }
                                .setNegativeButton(R.string.cancel_text) { dialog, _ -> dialog.cancel() }
                                .setOnCancelListener {
                                    viewModel.cancelSetQuality()
                                    AlertDialog.Builder(this)
                                        .setMessage(R.string.player_alert_enable_stream_with_mobile_data_cancel)
                                        .create()
                                        .show()
                                }
                                .create()
                                .show()
                        }
                        else -> {
                        }
                    }
                }
                Status.ERROR -> {
                    when (res.data) {
                        ResponseActionType.DO_NOTHING -> {
                            AlertDialog.Builder(this)
                                .setMessage(res.stringMessage)
                                .setPositiveButton(R.string.confirm_text) { _, _ -> }
                                .create()
                                .show()
                        }
                        else -> {
                        }
                    }
                }
            }
        })

        viewModel.refreshBookmarkedVid()

        // handle implicit intent from hyperlink
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { data ->
                val videoId: String? = when (data.host) {
                    "m.youtube.com", "www.youtube.com", "youtube.com" -> {
                        data.getQueryParameter("v")
                    }
                    "youtu.be" -> {
                        data.lastPathSegment
                    }
                    else -> null
                }
                // search video
                viewModel.playVideoForId(this, videoId, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mPlayerServiceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                playbackPosition = intent.getLongExtra(AudioPlayerService.EXTRA_KEY_PLAYBACK_POSITION, 0L)
                Log.d("MainActivity", "audio service broadcast receiver: $playbackPosition")
                viewModel.startPlayingVideo()
            }
        }
        registerReceiver(mPlayerServiceReceiver, IntentFilter().apply {
            addAction(AudioPlayerService.ACTION_GET_PLAYBACK_POSITION)
        })
        stopPlayService()
    }

    override fun onPause() {
        super.onPause()

        mPlayerServiceReceiver?.let { unregisterReceiver(it) }
        mPlayerServiceReceiver = null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            val wasPlaying = exoPlayer?.isPlaying == true
            exoPlayer?.apply {
                playWhenReady = false
                viewModel.saveWatchHistory(currentPosition)
                playbackPosition = currentPosition
            }
            if (isActivityOnStackTop() && wasPlaying && appSharedPreference.getBoolean(
                    PreferenceKey.ALLOW_PLAY_IN_BACKGROUND,
                    AppSettings.DEFAULT_ALLOW_PLAY_IN_BACKGROUND
                )
            ) {
                startPlayService()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val wasPlaying = exoPlayer?.isPlaying == true
            exoPlayer?.apply {
                playWhenReady = false
                viewModel.saveWatchHistory(currentPosition)
                playbackPosition = currentPosition
            }
            if (isActivityOnStackTop() && wasPlaying && appSharedPreference.getBoolean(
                    PreferenceKey.ALLOW_PLAY_IN_BACKGROUND,
                    AppSettings.DEFAULT_ALLOW_PLAY_IN_BACKGROUND
                )
            ) {
                startPlayService()
            }
        }
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onBackPressed() {
        if (mPlayerLayout.isFullscreen) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.SMALL)
            }
        } else if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            onBackPressedDispatcher.onBackPressed()
        } else if (shouldEnterPIPMode()) {
            if (!enterPIPMode()) super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        if (shouldEnterPIPMode()) {
            enterPIPMode()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                mPlayerLayout.setEnableTransition(!mPlayerLayout.isFullscreen)
                if (mPlayerLayout.isSmall) {
                    mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.FULLSCREEN)
                }
                if (mPlayerLayout.isFullscreen) hideSystemUI()
                else showSystemUI()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                mPlayerLayout.setEnableTransition(true)
                showSystemUI()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // set system ui visibility when activity window regain focus from dialog/popup
            when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> showSystemUI()
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (mPlayerLayout.isFullscreen) hideSystemUI()
                    else showSystemUI()
                }
            }
        }
    }

    override fun onSupportActionModeStarted(mode: ActionMode) {
        super.onSupportActionModeStarted(mode)
        getMainFragment()?.onSupportActionModeStarted(mode)
    }

    override fun onSupportActionModeFinished(mode: ActionMode) {
        super.onSupportActionModeFinished(mode)
        getMainFragment()?.onSupportActionModeFinished(mode)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isInPictureInPictureMode) {
                setPictureInPictureParams(PictureInPictureParams.Builder().apply {
                    if (exoPlayer?.isPlaying == true) {
                        setPauseAction()
                    } else {
                        setPlayAction()
                    }
                }.build())
                mPlayerLayout.hideControlView()
                mPipActionReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            ACTION_PIP_PLAY -> {
                                exoPlayer?.playWhenReady = true
                                setPictureInPictureParams(PictureInPictureParams.Builder()
                                    .setPauseAction()
                                    .build())
                            }
                            ACTION_PIP_PAUSE -> {
                                exoPlayer?.playWhenReady = false
                                setPictureInPictureParams(PictureInPictureParams.Builder()
                                    .setPlayAction()
                                    .build())
                            }
                        }
                    }
                }
                registerReceiver(mPipActionReceiver, IntentFilter().apply {
                    addAction(ACTION_PIP_PLAY)
                    addAction(ACTION_PIP_PAUSE)
                })
            } else {
                mPipActionReceiver?.let { unregisterReceiver(it) }
                mPipActionReceiver = null
            }
        }
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector(),
                object : DefaultLoadControl() {
                    override fun shouldContinueLoading(
                        bufferedDurationUs: Long,
                        playbackSpeed: Float
                    ): Boolean {
                        if (!allowPlayUsingMobile && !viewModel.isLocalBuffering && !isWifiConnected) return false
                        return super.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
                    }
                })
            mPlayerLayout.setPlayer(exoPlayer)
            exoPlayer!!.seekTo(currentWindow, playbackPosition)
            exoPlayer!!.addListener(mPlayerListener)
        }
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            playbackPosition = exoPlayer!!.currentPosition
            currentWindow = exoPlayer!!.currentWindowIndex
            exoPlayer!!.removeListener(mPlayerListener)
            exoPlayer!!.release()
            exoPlayer = null
        }
    }

    private fun setupViews() {
        mPlayerLayout.apply {
            toolbar.apply {
                inflateMenu(R.menu.player_option_menu)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.bookmark -> {
                            viewModel.videoData.value?.videoMeta?.videoMeta?.videoId?.let { vid ->
                                viewModel.setBookmarked(vid, true)
                            }
                        }
                        R.id.remove_bookmark -> {
                            viewModel.videoData.value?.videoMeta?.videoMeta?.videoId?.let { vid ->
                                viewModel.setBookmarked(vid, false)
                            }
                        }
                        R.id.rotation -> {
                            handleRotation()
                        }
                        R.id.other_action -> {
                            mOptionDialog?.show()
                        }
                    }
                    true
                }
                setBookmarkButton(false)        // set visibility before view data is loaded
            }
            setPlayerSizeListener(object : VideoPlayerLayout.PlayerSizeListener {
                @SuppressLint("SourceLockedOrientationActivity")
                override fun onStart(
                    start: VideoPlayerLayout.PlayerSize,
                    end: VideoPlayerLayout.PlayerSize
                ) {
                    mPlayerSizeListeners.forEach { it.onStart(start, end) }
                    if (start == VideoPlayerLayout.PlayerSize.DISMISS && end == VideoPlayerLayout.PlayerSize.FULLSCREEN) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                    }
                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onComplete(current: VideoPlayerLayout.PlayerSize) {
                    // when there is transition, check if player size is fullscreen,
                    // if true then disable user transition when orientation is not portrait
                    mPlayerSizeListeners.forEach { it.onComplete(current) }
                    when (current) {
                        VideoPlayerLayout.PlayerSize.FULLSCREEN -> {
                            val orientation = resources.configuration.orientation
                            setEnableTransition(orientation == Configuration.ORIENTATION_PORTRAIT)
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) hideSystemUI()
                            else showSystemUI()
                        }
                        VideoPlayerLayout.PlayerSize.SMALL -> {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                            showSystemUI()
                        }
                        VideoPlayerLayout.PlayerSize.DISMISS -> {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            showSystemUI()
                        }
                    }
                }
            })
        }
        mOptionDialog = PlayerBottomSheetDialogBuilder.createDialog(this) {
            it.recycler_view.apply {
                adapter = mOptionAdapter.apply { submitList(extraPlayerOptions) }
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
        mVideoInfoDialog = VideoInfoBottomSheetDialog(this)
        mResOptionDialog = VideoQualityOptionDialog(this).apply {
            setOnQualityOptionSelectedListener(object : VideoQualityOptionDialog.OnQualityOptionSelectedListener {
                override fun onQualityOptionSelected(vTag: Int, aTag: Int) {
                    // save playback state
                    exoPlayer?.let {
                        currentWindow = it.currentWindowIndex
                        playbackPosition = it.currentPosition
                    }
                    viewModel.setQuality(vTag, aTag)
                }
            })
        }
        mArchiveOptionDialog = ArchiveOptionDialog(this).apply {
            setOnArchiveOptionSelectedListener(object : ArchiveOptionDialog.OnArchiveOptionSelectedListener {
                override fun onArchiveOptionSelected(vTag: Int, aTag: Int) {
                    viewModel.archiveVideo(this@MainActivity, vTag, aTag)
                }
            })
        }
        mPlaybackSpeedDialog = PlaybackSpeedDialog(
            this
        ).apply {
            setOnPlaybackSpeedChangeListener(object : PlaybackSpeedDialog.OnPlaybackSpeedChangeListener {
                override fun onPlaybackSpeedChange(newPlaybackSpeed: Float) {
                    viewModel.setPlaybackSpeed(newPlaybackSpeed)
                }
            })
        }
        mLoadingDialog = ProgressDialogUtil.create(this)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun handleRotation() {
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
        }
    }

    private fun setBookmarkButton(isBookmarked: Boolean) {
        val toolbar = mPlayerLayout.toolbar
        val btnBookmark = toolbar.menu.findItem(R.id.bookmark)
        val btnRemoveBookmark = toolbar.menu.findItem(R.id.remove_bookmark)
        btnBookmark.isVisible = !isBookmarked
        btnRemoveBookmark.isVisible = isBookmarked
    }

    fun playVideoForId(videoId: String, forceFindFile: Boolean = false) {
//        playWhenReady = true
        playbackPosition = 0
//        currentWindow = 0
//        exoPlayer?.let {
//            it.playWhenReady = false
//            it.seekTo(currentWindow, 0)
//            it.playbackParameters = viewModel.playbackParams.value!!
//        }
        viewModel.playVideoForId(
            this, videoId, forceFindFile ||
                    appSharedPreference.getBoolean(
                        PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST,
                        AppSettings.DEFAULT_PLAYER_PLAY_DOWNLOADED_IF_EXIST
                    )
        )
    }

    fun showArchiveOption(videoId: String) {
        viewModel.showArchiveOption(videoId)
    }

    private fun showPlayerView() {
        mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.FULLSCREEN)
    }

    private fun dismissPlayerView() {
        mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.DISMISS)
    }

    fun addPlayerSizeListener(listener: VideoPlayerLayout.PlayerSizeListener) {
        mPlayerSizeListeners.add(listener)
    }

    fun removePlayerSizeListener(listener: VideoPlayerLayout.PlayerSizeListener) {
        mPlayerSizeListeners.remove(listener)
    }

    private fun prepareMediaResource(vararg uri: Uri?) {
        uri.mapNotNull { progressiveSrcFactory.createMediaSource(it) }.apply {
            when (size) {
                0 -> return
                1 -> exoPlayer?.prepare(first())
                else -> exoPlayer?.prepare(MergingMediaSource(*toTypedArray()))
            }
//            exoPlayer?.playWhenReady = playWhenReady
//            exoPlayer?.seekTo(currentWindow, playbackPosition)
        }
    }

    private fun getResFileUriIfExist(filename: String): Uri? {
        val file = File(FileDownloadHelper.getDir(this), filename)
        return if (file.exists()) file.toUri() else null
    }

    private fun shouldEnterPIPMode(): Boolean {
        // show on top of other app according to user preference
        if (AppSettings.isPipSupported(this) && appSharedPreference.getBoolean(
                PreferenceKey.PLAYER_ENABLE_PIP,
                AppSettings.DEFAULT_PLAYER_ENABLE_PIP
            )
        ) return exoPlayer?.isPlaying ?: false
        return false
    }

    private fun enterPIPMode(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return enterPictureInPictureMode(
                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            )
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun PictureInPictureParams.Builder.setPauseAction(): PictureInPictureParams.Builder =
        setActions(listOf(RemoteAction(Icon.createWithResource(this@MainActivity,
            R.drawable.exo_controls_pause), "Pause", "Pause video",
            PendingIntent.getBroadcast(this@MainActivity, REQUEST_CODE_PIP_PAUSE,
                Intent(ACTION_PIP_PAUSE), 0))))

    @RequiresApi(Build.VERSION_CODES.O)
    private fun PictureInPictureParams.Builder.setPlayAction(): PictureInPictureParams.Builder =
        setActions(listOf(RemoteAction(Icon.createWithResource(this@MainActivity,
            R.drawable.exo_controls_play), "Play", "Play video",
            PendingIntent.getBroadcast(this@MainActivity, REQUEST_CODE_PIP_PLAY,
                Intent(ACTION_PIP_PLAY), 0))))

    private fun startPlayService() {
        val viewData = viewModel.videoData.value!!
        val intent = AudioPlayerService.IntentBuilder(this)
            .setUriList(viewModel.resourceUri.value!!.uriList.toTypedArray())
            .setPlaybackPosition(playbackPosition)
            .setTitle(viewData.videoMeta.videoMeta.title)
            .setContent(viewData.videoMeta.videoMeta.channelTitle)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopPlayService() {
        stopService(Intent(this, AudioPlayerService::class.java))
    }

    fun getMainFragment(): MainPageFragment? =
        supportFragmentManager.findFragmentById(R.id.root_nav_container)?.
            childFragmentManager?.fragments?.firstOrNull() as? MainPageFragment

    companion object {
        const val TAG = "MainActivity"
        const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.extra.videoId"
        private const val REQUEST_CODE_PIP_PLAY = 10001
        private const val REQUEST_CODE_PIP_PAUSE = 10002
        private const val ACTION_PIP_PLAY = "com.amoscyk.android.rewatchplayer.PIP_PLAY"
        private const val ACTION_PIP_PAUSE = "com.amoscyk.android.rewatchplayer.PIP_PAUSE"
        private val SELECTION_DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<PlayerSelection<MainViewModel.VideoQualitySelection>>() {
            override fun areItemsTheSame(
                oldItem: PlayerSelection<MainViewModel.VideoQualitySelection>,
                newItem: PlayerSelection<MainViewModel.VideoQualitySelection>
            ) = oldItem.item == newItem.item && oldItem.selected == newItem.selected

            override fun areContentsTheSame(
                oldItem: PlayerSelection<MainViewModel.VideoQualitySelection>,
                newItem: PlayerSelection<MainViewModel.VideoQualitySelection>
            ) = oldItem == newItem
        }
    }
}
