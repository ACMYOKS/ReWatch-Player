package com.amoscyk.android.rewatchplayer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
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
import android.os.Handler
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.amoscyk.android.rewatchplayer.*
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.UpdateResponse
import com.amoscyk.android.rewatchplayer.service.AudioPlayerService
import com.amoscyk.android.rewatchplayer.ui.library.LibraryFragmentDirections
import com.amoscyk.android.rewatchplayer.ui.player.*
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.SnackbarControl
import com.amoscyk.android.rewatchplayer.util.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import org.json.JSONObject
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.net.URL


class MainActivity : ReWatchPlayerActivity(), EasyPermissions.PermissionCallbacks {

    data class Resolution(val itag: Int, val quality: String) : SelectableItemWithTitle {
        override fun getTitle() = quality
    }

    private var mLoadingDialog: AlertDialog? = null

    // player option view
    private var mOptionDialog: BottomSheetDialog? = null
    private var mVideoInfoDialog: VideoInfoBottomSheetDialog? = null
    private var mResOptionDialog2: BottomSheetDialog? = null
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
            PlayerOption(getString(R.string.player_option_resolution), R.drawable.ic_aspect_ratio_black_24dp),
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
    }
    private var mPlayerServiceReceiver: BroadcastReceiver? = null
    private var mApkDownloadReceiver: BroadcastReceiver? = null
    private var mDownloadCompleteReceiver: BroadcastReceiver? = null
    private var mPipActionReceiver: BroadcastReceiver? = null
    private var selectedITag: Int? = null       // save last selected itag for reselect tag on app restart

    private var apkDownloadId: Long? = null
    private var updateResponseToHandle: UpdateResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (rpApplication.getPlayer() == null) {
            val exoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector(),
                object : DefaultLoadControl() {
                    override fun shouldContinueLoading(
                        bufferedDurationUs: Long,
                        playbackSpeed: Float
                    ): Boolean {
                        // FIXME: value is undetermined when viewModel is released, try holding those values in application level
                        if (rpApplication.allowMobileStreaming.value == false &&
                            !viewModel.isLocalBuffering &&
                            rpApplication.isWifiConnected.value == false
                        ) return false
                        return super.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
                    }
                }).apply {
                addListener(mPlayerListener)
            }
            viewModel.onPlayerSetup(exoPlayer)
            rpApplication.setPlayer(exoPlayer)
        } else {
            viewModel.onPlayerSetup(rpApplication.getPlayer()!!)
        }

        // init account name for youtube repo every time main activity is created
        // to prevent null account name on app restart
        appSharedPreference.getString(PreferenceKey.ACCOUNT_NAME, null)?.let {
            viewModel.setAccountName(it)
        }

        setupViews()

        viewModel.skipForwardSecond.observe(this, Observer {
            getMainFragment()?.playerControl?.setFastForwardIncrementMs(it * 1000)
        })

        viewModel.skipBackwardSecond.observe(this, Observer {
            getMainFragment()?.playerControl?.setRewindIncrementMs(it * 1000)
        })

        viewModel.bookmarkedVid.observe(this, Observer {

        })

        viewModel.videoData.observe(this, Observer { viewData ->
            val meta = viewData.videoMeta
            getMainFragment()?.setTitle(meta.title)
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
            selectedITag = control.selectedTag
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
            mPlaybackSpeedAdvanceDialog?.setPlaybackMultiplier(it)
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

        viewModel.alertEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { ctrl ->
                ctrl.getBuilder(this).create().show()
            }
        })

        viewModel.autoSearchEvent.observe(this, Observer {
            getMainFragment()?.apply {
                navigateToPage(0)
                if (isFullscreen) setPlayerSize(MainPageFragment.PlayerSize.SMALL)
            }
            Handler().postDelayed({
                findNavController(R.id.main_page_nav_host_fragment).apply {
                    navigate(LibraryFragmentDirections.showVideoSearch().setVideoId(it), navOptions {
                        launchSingleTop = false     // multi-instance is better
                    })
                }
            }, 0)

        })

        viewModel.updateDialogEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                updateResponseToHandle = it
                AppUpdateDialog(this, it) {
                   downloadApk()
                }.show()
            }
        })

        intent?.let { handleIntent(it) }
    }

    override fun onResume() {
        super.onResume()

        mPlayerServiceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val videoId = intent.getStringExtra(AudioPlayerService.EXTRA_KEY_VIDEO_ID)
                val vTag = intent.getIntExtra(AudioPlayerService.EXTRA_KEY_VIDEO_ITAG, 0)
                val aTag = intent.getIntExtra(AudioPlayerService.EXTRA_KEY_AUDIO_ITAG, 0)
                if (videoId != null) {
                    if (viewModel.videoData.value?.videoMeta?.videoId != videoId) {
                        // no playing resource, restart killed app while foreground playing
                        // continue playing while setting data back for viewModel
                        viewModel.readyVideoBypass(videoId, vTag, aTag)
                    }
                }
            }
        }
        registerReceiver(mPlayerServiceReceiver, IntentFilter().apply {
            addAction(AudioPlayerService.ACTION_GET_PLAYBACK_POSITION)
        })
        mApkDownloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.getLongExtra(ApkDownloadHelper.EXTRA_DOWNLOAD_ID, -1).let {
                    if (it >= 0) {
                        apkDownloadId = it
                    }
                }
            }
        }
        registerReceiver(mApkDownloadReceiver, IntentFilter(ApkDownloadHelper.ACTION_GET_DOWNLOAD_ID))
        mDownloadCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)
                if (apkDownloadId == downloadId) {
                    apkDownloadId = null
                    openDownloadedApk(downloadId)
                }
            }
        }
        registerReceiver(mDownloadCompleteReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        stopPlayService()
    }

    override fun onPause() {
        super.onPause()

        mPlayerServiceReceiver?.let { unregisterReceiver(it) }
        mPlayerServiceReceiver = null
        mApkDownloadReceiver?.let { unregisterReceiver(it) }
        mApkDownloadReceiver = null
        mDownloadCompleteReceiver?.let { unregisterReceiver(it) }
        mDownloadCompleteReceiver = null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            getPlayer()?.apply {
                if (isActivityOnStackTop() && isPlaying && appSharedPreference.getBoolean(
                        PreferenceKey.ALLOW_PLAY_IN_BACKGROUND,
                        AppSettings.DEFAULT_ALLOW_PLAY_IN_BACKGROUND
                    )
                ) {
                    startPlayService()
                } else {
                    playWhenReady = false
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getPlayer()?.apply {
                if (isActivityOnStackTop() && isPlaying && appSharedPreference.getBoolean(
                        PreferenceKey.ALLOW_PLAY_IN_BACKGROUND,
                        AppSettings.DEFAULT_ALLOW_PLAY_IN_BACKGROUND
                    )
                ) {
                    startPlayService()
                } else {
                    playWhenReady = false
                }
            }
        }
    }

    override fun onDestroy() {
        rpApplication.getPlayer()?.removeListener(mPlayerListener)
        if (!isMyServiceRunning(AudioPlayerService::class.java)) {
            rpApplication.releasePlayer()
        }
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_WRITE_STORAGE_PERM) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                AppSettingsDialog.Builder(this).build().show()
            }
        }
    }

    fun getPlayer(): ExoPlayer? = rpApplication.getPlayer()

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
        mPlaybackSpeedAdvanceDialog = PlaybackSpeedAdvanceDialog(this).apply {
            setOnPlaybackSpeedChangeListener {
                viewModel.setPlaybackSpeed(it)
            }
        }
        mVideoInfoDialog = VideoInfoBottomSheetDialog(this)
        mLoadingDialog = ProgressDialogUtil.create(this)
    }

    fun getPlayerOptionDialog() = mOptionDialog

    private fun showPlayerView() {
        getMainFragment()?.setPlayerSize(MainPageFragment.PlayerSize.FULLSCREEN)
    }

    private fun dismissPlayerView() {
        getMainFragment()?.setPlayerSize(MainPageFragment.PlayerSize.DISMISS)
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
        viewModel.videoData.value?.let { viewData ->
            val intent = AudioPlayerService.IntentBuilder(this)
                .setVideoId(viewData.videoMeta.videoId)
                .setTitle(viewData.videoMeta.title)
                .setContent(viewData.videoMeta.channelTitle)
                .setVTag(selectedITag ?: 0)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun stopPlayService() {
        stopService(Intent(this, AudioPlayerService::class.java))
    }

    private fun handleIntent(intent: Intent) {
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
                        viewModel.autoSearchVideo(videoId)
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
                                viewModel.autoSearchVideo(videoId)
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

    fun getMainFragment(): MainPageFragment? =
        supportFragmentManager.findFragmentById(R.id.root_nav_container)?.
            childFragmentManager?.fragments?.firstOrNull() as? MainPageFragment

    @AfterPermissionGranted(REQUEST_WRITE_STORAGE_PERM)
    private fun downloadApk() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            updateResponseToHandle?.let {
                ApkDownloadHelper.startDownloadApk(this, it)
                updateResponseToHandle = null
            }
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.settings_request_write_storage_permission),
                REQUEST_WRITE_STORAGE_PERM,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun openDownloadedApk(downloadId: Long) {
        val dlMgr = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dlMgr.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val localUriStr = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val mimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
            if (status == DownloadManager.STATUS_SUCCESSFUL && localUriStr != null) {
                var localUri = Uri.parse(localUriStr)
                if (ContentResolver.SCHEME_FILE == localUri.scheme) {
                    val file = File(localUri.path!!)
                    localUri = FileProvider.getUriForFile(this, "com.amoscyk.android.rewatchplayer.provider", file)
                }
                val openFileIntent = Intent(Intent.ACTION_VIEW)
                openFileIntent
                    .setDataAndType(localUri, mimeType)
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    startActivity(openFileIntent)
                } catch (e: Exception) {
                    getMainFragment()?.showSnackbar(R.string.update_download_cannot_open_downloaded_apk, Snackbar.LENGTH_SHORT)
                }
            }
        }
        cursor.close()
    }

    companion object {
        const val TAG = "MainActivity"
        const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.extra.videoId"
        private const val REQUEST_CODE_PIP_PLAY = 10001
        private const val REQUEST_CODE_PIP_PAUSE = 10002
        private const val REQUEST_WRITE_STORAGE_PERM = 20000
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
