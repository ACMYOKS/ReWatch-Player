package com.amoscyk.android.rewatchplayer.ui

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.net.toUri
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
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.SnackbarControl
import com.amoscyk.android.rewatchplayer.util.*
import com.google.android.exoplayer2.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import java.io.File
import java.net.URL


class MainActivity : ReWatchPlayerActivity() {

    data class Resolution(val itag: Int, val quality: String) : SelectableItemWithTitle {
        override fun getTitle() = quality
    }

    private var mLoadingDialog: AlertDialog? = null

    // player option view
    private var mOptionDialog: BottomSheetDialog? = null
    private var mVideoInfoDialog: VideoInfoBottomSheetDialog? = null
    private var mArchiveOptionDialog: ArchiveOptionDialog? = null
    private var mResOptionDialog: VideoQualityOptionDialog? = null
    private var mResOptionDialog2: BottomSheetDialog? = null
    private var mPlaybackSpeedDialog: PlaybackSpeedDialog? = null
    private var mPlaybackSpeedDialog2: PlaybackSpeedDialog2? = null
    private var mPlaybackSpeedAdvanceDialog: PlaybackSpeedAdvanceDialog? = null

    // player option view related class
    private val mOptionAdapter = PlayerOptionAdapter({ option, position ->
        mOptionDialog?.dismiss()
        if (position == 0) {
            mVideoInfoDialog?.show()
        } else if (position == 1) {
            mResOptionDialog2?.show()
        } else if (position == 2) {
            viewModel.videoData.value?.let { videoData ->
                viewModel.readyArchive(videoData.videoMeta.videoId)
            }
        } else if (position == 3) {
            mPlaybackSpeedAdvanceDialog?.show()
        } else if (position == 4) {
            viewModel.videoData.value?.let { videoData ->
                Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT, "https://youtu.be/" +
                                videoData.videoMeta.videoId
                    )
                    type = "text/plain"
                }, getString(R.string.player_share_title)).also { startActivity(it) }
            }

        }
    })

    private val extraPlayerOptions by lazy {
        listOf(
            PlayerOption(getString(R.string.player_option_info), R.drawable.ic_info),
            PlayerOption(
                getString(R.string.player_option_resolution),
                R.mipmap.ic_action_resolution
            ),
            PlayerOption(getString(R.string.player_option_archive), R.drawable.ic_archive_white),
            PlayerOption(getString(R.string.player_option_speed), R.drawable.ic_slow_motion_video),
            PlayerOption(getString(R.string.player_option_share), R.drawable.ic_share)
        )
    }
    private val resSelectionAdapter = PlayerSelectionAdapter({
        mResOptionDialog2?.dismiss()
        viewModel.changeQualityV2(it.item.itag)
    }, object : DiffUtil.ItemCallback<PlayerSelection<Resolution>>() {
        override fun areItemsTheSame(
            oldItem: PlayerSelection<Resolution>,
            newItem: PlayerSelection<Resolution>
        ) = oldItem.item == newItem.item && oldItem.selected == newItem.selected

        override fun areContentsTheSame(
            oldItem: PlayerSelection<Resolution>,
            newItem: PlayerSelection<Resolution>
        ) = oldItem == newItem
    })

    private val viewModel by viewModels<MainViewModel> { viewModelFactory }

    private val mPlayerListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isInPictureInPictureMode) {
                    setPictureInPictureParams(
                        PictureInPictureParams.Builder().apply {
                            if (isPlaying) setPauseAction()
                            else setPlayAction()
                        }.build()
                    )
                }
            }
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                getPlayer()?.apply {
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

        viewModel.initPlayer(this)
        getPlayer()?.addListener(mPlayerListener)
        setupViews()

        viewModel.skipForwardSecond.observe(this, Observer {
            getMainFragment()?.playerControl?.setFastForwardIncrementMs(it * 1000)
        })

        viewModel.skipBackwardSecond.observe(this, Observer {
            getMainFragment()?.playerControl?.setRewindIncrementMs(it * 1000)
        })

        viewModel.needShowArchiveOption.observe(this, Observer { event ->
            event.getContentIfNotHandled { mArchiveOptionDialog?.show() }
        })

        // FIXME: change intent handling, show list instead of direct showing
        viewModel.searchVideoResource.observe(this, Observer { res ->
            when (res.status) {
                Status.LOADING -> {
                    Toast.makeText(this, "Checking...", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    viewModel.readyVideo(res.data!!)
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

        viewModel.bookmarkedVid.observe(this, Observer {

        })

        viewModel.videoData.observe(this, Observer { viewData ->
            val meta = viewData.videoMeta
            getMainFragment()?.setTitle(meta.title)
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
            mVideoInfoDialog?.apply {
                setVideoMeta(meta)
                setViewChannelButtonOnClickListener(View.OnClickListener {
                    dismiss()
                    findNavController(R.id.main_page_nav_host_fragment)
                        .navigate(LibraryFragmentDirections.showChannelDetail(meta.channelId))
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        toggleLockedRotation()
                    }
                    getMainFragment()?.setPlayerSize(MainPageFragment.PlayerSize.SMALL)
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

        viewModel.itagControl.observe(this, Observer { control ->
            val list = control.availableTag.map { itag ->
                PlayerSelection(
                    Resolution(
                        itag,
                        YouTubeStreamFormatCode.MUX_FORMAT_MAP[itag]
                            ?: YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMAT_MAP[itag].orEmpty()
                    ),
                    itag == control.selectedTag
                )
            }
            resSelectionAdapter.submitList(list)
        })

        viewModel.archiveEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { control ->
                ArchiveOptionDialog2(this, control.availableTag).apply {
                    setListener { itag -> control.selection(itag, null) }       // TODO: add support for audio only
                }.show()
            }
        })

        viewModel.playbackSpeedMultiplier.observe(this, Observer {
            //            mPlaybackSpeedDialog2?.setPlaybackMultiplier(it)
            mPlaybackSpeedAdvanceDialog?.setPlaybackMultiplier(it)
        })

        viewModel.resourceUri.observe(this, Observer {
            showPlayerView()
            prepareMediaResource(*it.uriList.toTypedArray())
            if (it.lastPlayPos != null) {
                AlertDialog.Builder(this)
                    .setMessage(R.string.player_restore_last_play_position_message)
                    .setPositiveButton(R.string.confirm_text) { d, i ->
                        getPlayer()?.apply {
                            playWhenReady = true
//                            seekTo(currentWindow, it.lastPlayPos)
                        }
                    }
                    .setNegativeButton(R.string.negative_text) { d, i ->
                        d.cancel()
                    }
                    .setOnCancelListener {
                        getPlayer()?.apply {
                            playWhenReady = true
//                            seekTo(currentWindow, 0)
                        }
                    }
                    .create()
                    .show()
            } else {
                getPlayer()?.apply {
                    playWhenReady = true
//                    seekTo(currentWindow, playbackPosition)
                }
            }
        })

        viewModel.shouldSaveWatchHistory.observe(this, Observer { event ->
            event.getContentIfNotHandled { videoId ->
                getPlayer()?.let {
                    Log.d("MainActivity", "save watch history, ${videoId}, ${it.currentPosition}")
                    viewModel.saveWatchHistory(videoId, it.currentPosition)
                }
            }
        })

        viewModel.snackEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { ctrl ->
                val frag = getMainFragment() ?: return@getContentIfNotHandled
                val snackbar = frag.newSnackbar(ctrl.title, when (ctrl.duration) {
                    SnackbarControl.Duration.SHORT -> Snackbar.LENGTH_SHORT
                    SnackbarControl.Duration.LONG -> Snackbar.LENGTH_LONG
                    SnackbarControl.Duration.FOREVER -> Snackbar.LENGTH_INDEFINITE
                })
                ctrl.action?.let { action ->
                    snackbar.setAction(action.title) { action.action }
                }
                snackbar.show()
            }
        })

        viewModel.archiveResult.observe(this, Observer { result ->
            when (result.status) {
                Status.SUCCESS -> {
                    getMainFragment()?.showSnackbar(
                        result.data!!.taskCount.let {
                            if (it > 0) {
                                getString(R.string.player_archive_new_tasks).format(it)
                            } else {
                                getString(R.string.player_archive_no_new_tasks)
                            }
                        },
                        Snackbar.LENGTH_SHORT
                    )
                }
                Status.ERROR -> {
                    getMainFragment()?.showSnackbar(
                        result.stringMessage,
                        Snackbar.LENGTH_SHORT
                    )
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

        viewModel.showPlayerView.observe(this, Observer { event ->
            event.getContentIfNotHandled { show ->
                if (show) showPlayerView()
            }
        })

        viewModel.shouldStartPlayVideo.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                getPlayer()?.let {
                    it.playWhenReady = true
//                    it.seekTo(currentWindow, playbackPosition)
                }
            }
        })

        viewModel.shouldStopVideo.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                getPlayer()?.playWhenReady = false
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

        viewModel.alertEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { ctrl ->
                ctrl.getBuilder(this).create().show()
            }
        })

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

        // FIXME: review intent handling
        // handle implicit intent from hyperlink
        when (intent.action) {
            Intent.ACTION_VIEW -> {
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
                    if (videoId != null) {
                        viewModel.readyVideo(videoId)
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle(R.string.player_error_title)
                            .setMessage("Cannot find videoId")
                            .create()
                            .show()
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                        runCatching {
                            val url = URL(text)
                            val videoId: String? = when (url.host) {
                                "m.youtube.com", "www.youtube.com", "youtube.com" -> {
                                    url.query.split("&").firstOrNull { it.contains("v=") }
                                        ?.replace("v=", "")
                                }
                                "youtu.be" -> {
                                    url.path.split("/").lastOrNull()
                                }
                                else -> null
                            }
                            // search video
                            if (videoId != null) {
                                viewModel.readyVideo(videoId)
                            } else {
                                AlertDialog.Builder(this)
                                    .setTitle(R.string.player_error_title)
                                    .setMessage("Cannot find videoId")
                                    .create()
                                    .show()
                            }
                        }.onFailure {
                            AlertDialog.Builder(this)
                                .setTitle(R.string.player_error_title)
                                .setMessage(R.string.player_error_malformed_url)
                                .create()
                                .show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mPlayerServiceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val playbackPos =
                    intent.getLongExtra(AudioPlayerService.EXTRA_KEY_PLAYBACK_POSITION, 0L)
                val videoId = intent.getStringExtra(AudioPlayerService.EXTRA_KEY_VIDEO_ID)
                if (viewModel.videoData.value?.videoMeta?.videoId != videoId) {
                    // no playing resource, restart killed app while foreground playing
                    viewModel.readyVideo(videoId)
                } else {
//                    playbackPosition = playbackPos
//                    Log.d("MainActivity", "audio service broadcast receiver: $playbackPosition")
                    viewModel.startPlayingVideo()
                }
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
            val wasPlaying = getPlayer()?.isPlaying == true
            getPlayer()?.apply {
                playWhenReady = false
                viewModel.saveWatchHistory(currentPosition)
//                playbackPosition = currentPosition
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
            val wasPlaying = getPlayer()?.isPlaying == true
            getPlayer()?.apply {
                playWhenReady = false
                viewModel.saveWatchHistory(currentPosition)
//                playbackPosition = currentPosition
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
        getPlayer()?.removeListener(mPlayerListener)
        viewModel.releasePlayer()
        super.onDestroy()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onBackPressed() {
        val mainFrag = getMainFragment()
        if (mainFrag != null && mainFrag.isFullscreen) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            } else {
                mainFrag.setPlayerSize(MainPageFragment.PlayerSize.SMALL)
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            // set system ui visibility when activity window regain focus from dialog/popup
            when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> showSystemUI()
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (getMainFragment()?.isFullscreen == true) hideSystemUI()
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
                    if (getPlayer()?.isPlaying == true) {
                        setPauseAction()
                    } else {
                        setPlayAction()
                    }
                }.build())
                getMainFragment()?.hideControlView()
                mPipActionReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            ACTION_PIP_PLAY -> {
                                getPlayer()?.playWhenReady = true
                                setPictureInPictureParams(
                                    PictureInPictureParams.Builder()
                                        .setPauseAction()
                                        .build()
                                )
                            }
                            ACTION_PIP_PAUSE -> {
                                getPlayer()?.playWhenReady = false
                                setPictureInPictureParams(
                                    PictureInPictureParams.Builder()
                                        .setPlayAction()
                                        .build()
                                )
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

    fun getPlayer(): ExoPlayer? = viewModel.getPlayer()

    private fun setupViews() {
        mOptionDialog = PlayerBottomSheetDialogBuilder.createDialog(this) {
            it.recycler_view.apply {
                adapter = mOptionAdapter.apply { submitList(extraPlayerOptions) }
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
        mResOptionDialog2 = PlayerBottomSheetDialogBuilder.createDialog(this) {
            it.recycler_view.apply {
                adapter = resSelectionAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
//        mPlaybackSpeedDialog2 = PlaybackSpeedDialog2(this).apply {
//            setOnPlaybackSpeedChangeListener {
//                viewModel.setPlaybackSpeed(it)
//            }
//            setOnAdvanceClickListener(DialogInterface.OnClickListener { dialog, which ->
//                mPlaybackSpeedAdvanceDialog?.show()
//            })
//        }
        mPlaybackSpeedAdvanceDialog = PlaybackSpeedAdvanceDialog(this).apply {
            setOnPlaybackSpeedChangeListener {
                viewModel.setPlaybackSpeed(it)
            }
        }
        mVideoInfoDialog = VideoInfoBottomSheetDialog(this)
        mResOptionDialog = VideoQualityOptionDialog(this).apply {
            setOnQualityOptionSelectedListener(object :
                VideoQualityOptionDialog.OnQualityOptionSelectedListener {
                override fun onQualityOptionSelected(vTag: Int, aTag: Int) {
                    // save playback state
                    getPlayer()?.let {
                        //                        currentWindow = it.currentWindowIndex
//                        playbackPosition = it.currentPosition
                    }
                    viewModel.setQuality(vTag, aTag)
                }
            })
        }
//        mArchiveOptionDialog = ArchiveOptionDialog(this).apply {
//            setOnArchiveOptionSelectedListener(object :
//                ArchiveOptionDialog.OnArchiveOptionSelectedListener {
//                override fun onArchiveOptionSelected(vTag: Int, aTag: Int) {
//                    viewModel.archiveVideo(this@MainActivity, vTag, aTag)
//                }
//            })
//        }
        mPlaybackSpeedDialog = PlaybackSpeedDialog(
            this
        ).apply {
            setOnPlaybackSpeedChangeListener(object :
                PlaybackSpeedDialog.OnPlaybackSpeedChangeListener {
                override fun onPlaybackSpeedChange(newPlaybackSpeed: Float) {
                    viewModel.setPlaybackSpeed(newPlaybackSpeed)
                }
            })
        }
        mLoadingDialog = ProgressDialogUtil.create(this)
    }

//    fun playVideoForId(videoId: String, forceFindFile: Boolean = false) {
//        playWhenReady = true
//        playbackPosition = 0
//        currentWindow = 0
//        exoPlayer?.let {
//            it.playWhenReady = false
//            it.seekTo(currentWindow, 0)
//            it.playbackParameters = viewModel.playbackParams.value!!
//        }
//        viewModel.playVideoForId(
//            this, videoId, forceFindFile ||
//                    appSharedPreference.getBoolean(
//                        PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST,
//                        AppSettings.DEFAULT_PLAYER_PLAY_DOWNLOADED_IF_EXIST
//                    )
//        )
//    }

    fun getPlayerOptionDialog() = mOptionDialog

    private fun showPlayerView() {
        getMainFragment()?.setPlayerSize(MainPageFragment.PlayerSize.FULLSCREEN)
    }

    private fun dismissPlayerView() {
        getMainFragment()?.setPlayerSize(MainPageFragment.PlayerSize.DISMISS)
    }


    private fun prepareMediaResource(vararg uri: Uri?) {
//        uri.mapNotNull { progressiveSrcFactory.createMediaSource(it) }.apply {
//            when (size) {
//                0 -> return
//                1 -> getPlayer()?.prepare(first())
//                else -> getPlayer()?.prepare(MergingMediaSource(*toTypedArray()))
//            }
//            exoPlayer?.playWhenReady = playWhenReady
//            exoPlayer?.seekTo(currentWindow, playbackPosition)
//        }
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
        ) return getPlayer()?.isPlaying ?: false
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
        setActions(
            listOf(
                RemoteAction(
                    Icon.createWithResource(
                        this@MainActivity,
                        R.drawable.exo_controls_pause
                    ), "Pause", "Pause video",
                    PendingIntent.getBroadcast(
                        this@MainActivity, REQUEST_CODE_PIP_PAUSE,
                        Intent(ACTION_PIP_PAUSE), 0
                    )
                )
            )
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun PictureInPictureParams.Builder.setPlayAction(): PictureInPictureParams.Builder =
        setActions(
            listOf(
                RemoteAction(
                    Icon.createWithResource(
                        this@MainActivity,
                        R.drawable.exo_controls_play
                    ), "Play", "Play video",
                    PendingIntent.getBroadcast(
                        this@MainActivity, REQUEST_CODE_PIP_PLAY,
                        Intent(ACTION_PIP_PLAY), 0
                    )
                )
            )
        )

    private fun startPlayService() {
        val viewData = viewModel.videoData.value!!
        val intent = AudioPlayerService.IntentBuilder(this)
            .setVideoId(viewData.videoMeta.videoId)
            .setUriList(viewModel.resourceUri.value!!.uriList.toTypedArray())
//            .setPlaybackPosition(playbackPosition)
            .setTitle(viewData.videoMeta.title)
            .setContent(viewData.videoMeta.channelTitle)
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
        supportFragmentManager.findFragmentById(R.id.root_nav_container)?.childFragmentManager?.fragments?.firstOrNull() as? MainPageFragment

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
