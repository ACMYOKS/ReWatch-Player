package com.amoscyk.android.rewatchplayer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.MainViewModel.ResponseActionType
import com.amoscyk.android.rewatchplayer.ui.account.StartupAccountFragmentDirections
import com.amoscyk.android.rewatchplayer.ui.library.LibraryFragmentDirections
import com.amoscyk.android.rewatchplayer.ui.player.*
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import kotlinx.android.synthetic.main.dialog_archive_option.view.*
import java.io.File


class MainActivity : ReWatchPlayerActivity() {

    private val mPlayerLayout by lazy { findViewById<VideoPlayerLayout>(R.id.video_player_layout) }

    // player option view
    private lateinit var mOptionDialog: BottomSheetDialog
    private var mVideoInfoDialog: VideoInfoBottomSheetDialog? = null
    private var mArchiveOptionDialog: ArchiveOptionDialog? = null
    private var mResOptionDialog: VideoQualityOptionDialog? = null
    private var mPlaybackSpeedDialog: PlaybackSpeedDialog? = null

    private var mVtagList = LinkedHashMap<Int, YouTubeStreamFormatCode.StreamFormat>()
    private var mAtagList = LinkedHashMap<Int, YouTubeStreamFormatCode.StreamFormat>()

    // player option view related class
    private val mOptionAdapter = PlayerOptionAdapter({ option, position ->
        if (position == 0) {
            mVideoInfoDialog?.show()
            mOptionDialog.dismiss()
        } else if (position == 1) {
            mResOptionDialog?.show()
            mOptionDialog.dismiss()
        } else if (position == 2) {
            viewModel.videoMeta.value?.videoId?.let { vid ->
                showArchiveOption(vid)
                mOptionDialog.dismiss()
            }
        } else if (position == 3) {
            viewModel.playbackParams.value?.speed?.let { speed ->
                mPlaybackSpeedDialog?.apply {
                    setPlaybackMultiplier(speed)
                    show()
                }
            }
            mOptionDialog.dismiss()
        } else if (position == 3) {
            mOptionDialog.dismiss()
            Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" + viewModel.videoMeta.value!!.videoId)
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

    private var videoId: String? = null

    private var isWifiConnected = false
    private var isMobileDataConnected = false
    private var allowPlayUsingWifiOnly = false

    private var exoPlayer: SimpleExoPlayer? = null
    private var playWhenReady = true
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
    }

    private var mPlayerSizeListeners = ArrayList<VideoPlayerLayout.PlayerSizeListener>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        SPBooleanLiveData(appSharedPreference,
            PreferenceKey.PLAYER_ONLY_PLAY_WHEN_USING_WIFI,
            false).apply {
            observe(this@MainActivity, Observer {
                allowPlayUsingWifiOnly = it
                viewModel.notifyIsAllowedPlayUsingWifiOnly(allowPlayUsingWifiOnly)
            })
        }

        viewModel.needShowArchiveOption.observe(this, Observer { event ->
            event.getContentIfNotHandled { mArchiveOptionDialog?.show() }
        })

        viewModel.bookmarkToggled.observe(this, Observer {
            viewModel.videoMeta.value?.apply {
                bookmarked = !bookmarked
                setBookmarkButton(bookmarked)
            }
        })

        viewModel.searchVideoResource.observe(this, Observer { res ->
            when (res.status) {
                Status.LOADING -> {
                    Toast.makeText(this, "Checking...", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    viewModel.playVideoForId(this, res.data!!)
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

        viewModel.videoMeta.observe(this, Observer { meta ->
            mPlayerLayout.setTitle(meta.title)
            setBookmarkButton(meta.bookmarked)
            val itags = meta.itags
            mVtagList = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
                itags.contains(it.key)
            })
            mAtagList = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
                itags.contains(it.key)
            })
            mResOptionDialog?.apply {
                setVideoTags(mVtagList.keys.toList())
                setAudioVTags(mAtagList.keys.toList())
            }
            mArchiveOptionDialog?.apply {
                setVideoTags(mVtagList.keys.toList())
                setAudioVTags(mAtagList.keys.toList())
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

        viewModel.selectedTags.observe(this, Observer {
            mResOptionDialog?.apply {
                it.vTag?.let { setCurrentVTag(it) }
                it.aTag?.let { setCurrentATag(it) }
            }
        })

        viewModel.resourceUrl.observe(this, Observer {
            showPlayerView()
            prepareMediaResource(
                it.videoUrl?.let { url -> Uri.parse(url) },
                it.audioUrl?.let { url -> Uri.parse(url) }
            )
        })

        viewModel.resourceFile.observe(this, Observer {
            showPlayerView()
            prepareMediaResource(
                it.videoFile?.let { filename -> getResFileUriIfExist(filename) },
                it.audioFile?.let { filename -> getResFileUriIfExist(filename) }
            )
        })

        viewModel.archiveResult.observe(this, Observer { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    getMainFragment()?.contentContainer?.let {
                        Snackbar.make(
                            it,
                            "has new download task: ${result.data!!.taskCount}",
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
                                .setMessage(res.stringMessage)
                                .setPositiveButton("ok") { _, _ ->
                                    appSharedPreference.edit(true) {
                                        putBoolean(PreferenceKey.PLAYER_ONLY_PLAY_WHEN_USING_WIFI, false)
                                    }
                                    viewModel.continueSetQuality()
                                }
                                .setNegativeButton("cancel") { dialog, _ -> dialog.cancel() }
                                .setOnCancelListener { viewModel.cancelSetQuality() }
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
                                .setPositiveButton("ok") { _, _ -> }
                                .create()
                                .show()
                        }
                        else -> {
                        }
                    }
                }
            }
        })

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
                viewModel.searchVideoById(videoId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        releasePlayer()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onBackPressed() {
        if (mPlayerLayout.isFullscreen) {
            when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                Configuration.ORIENTATION_PORTRAIT -> mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.SMALL)
                else -> super.onBackPressed()
            }
        } else {
            super.onBackPressed()
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

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector(),
                object : DefaultLoadControl() {
                    override fun shouldContinueLoading(
                        bufferedDurationUs: Long,
                        playbackSpeed: Float
                    ): Boolean {
                        if (allowPlayUsingWifiOnly && !viewModel.isLocalBuffering && !isWifiConnected) return false
                        return super.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
                    }
                })
            mPlayerLayout.setPlayer(exoPlayer)
            exoPlayer!!.playWhenReady = playWhenReady
            exoPlayer!!.seekTo(currentWindow, playbackPosition)
            exoPlayer!!.addListener(mPlayerListener)
        }
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            playWhenReady = exoPlayer!!.playWhenReady
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
                        R.id.bookmark, R.id.remove_bookmark -> {
                            viewModel.videoMeta.value?.videoId?.let { vid ->
                                viewModel.toggleBookmarkStatus(vid)
                            }
                        }
                        R.id.rotation -> {
                            handleRotation()
                        }
                        R.id.other_action -> {
                            mOptionDialog.show()
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
                        playWhenReady = it.playWhenReady
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

    fun playVideoForId(videoId: String) {
        playWhenReady = true
        playbackPosition = 0
        currentWindow = 0
        exoPlayer?.let {
            it.playWhenReady = playWhenReady
            it.seekTo(currentWindow, playbackPosition)
            it.playbackParameters = viewModel.playbackParams.value!!
        }
        viewModel.playVideoForId(this, videoId)
    }

    fun showArchiveOption(videoId: String) {
        viewModel.showArchiveOption(videoId)
    }

    fun toggleBookmarkStatus(videoId: String) {
        viewModel.toggleBookmarkStatus(videoId)
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
            exoPlayer?.playWhenReady = playWhenReady
            exoPlayer?.seekTo(currentWindow, playbackPosition)
        }
    }

    private fun getResFileUriIfExist(filename: String): Uri? {
        val file = File(FileDownloadHelper.getDir(this), filename)
        return if (file.exists()) file.toUri() else null
    }

    fun getMainFragment(): MainPageFragment? =
        supportFragmentManager.findFragmentById(R.id.root_nav_container)?.
            childFragmentManager?.fragments?.firstOrNull() as? MainPageFragment

    companion object {
        const val TAG = "MainActivity"
        const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.extra.videoId"
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
