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
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.MainViewModel.ResponseActionType
import com.amoscyk.android.rewatchplayer.ui.player.*
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import kotlinx.android.synthetic.main.dialog_achive_option.view.*
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : ReWatchPlayerActivity() {

    private val mPlayerLayout by lazy { findViewById<VideoPlayerLayout>(R.id.video_player_layout) }

    // player option view
    private lateinit var mOptionDialog: BottomSheetDialog
    private lateinit var mResolutionDialog: BottomSheetDialog
    private lateinit var mAudioQualityDialog: BottomSheetDialog
    private var mResOptionDialog: AlertDialog? = null
    private var mResOptionDialogView: View? = null
    private var mArchiveOptionDialog: AlertDialog? = null
    private var mArchiveOptionDialogView: View? = null
    private lateinit var mResolutionRv: RecyclerView
    private lateinit var mQualityRv: RecyclerView

    private var mVtagList = LinkedHashMap<Int, YouTubeStreamFormatCode.StreamFormat>()
    private var mAtagList = LinkedHashMap<Int, YouTubeStreamFormatCode.StreamFormat>()

    // player option view related class
    private val mOptionAdapter = PlayerOptionAdapter({ option, position ->
        if (position == 0) {
//            mResolutionDialog.show()
            mResOptionDialog?.show()
            mOptionDialog.dismiss()
        } else if (position == 1) {
            mAudioQualityDialog.show()
            mOptionDialog.dismiss()
        }
    }, { isChecked, position ->
        if (position == 2) {

        }
    })
    private val mResSelectionAdapter = PlayerSelectionAdapter({
        mResolutionDialog.dismiss()
        exoPlayer?.let { player ->
            // save playback state
            playWhenReady = player.playWhenReady
            currentWindow = player.currentWindowIndex
            playbackPosition = player.currentPosition
        }
//        viewModel.setVideoFormat(true, it.item.itag)

    }, SELECTION_DIFF_CALLBACK)
    private val mAudioSelectionAdapter = PlayerSelectionAdapter({
        mAudioQualityDialog.dismiss()
        exoPlayer?.let { player ->
            // save playback state
            playWhenReady = player.playWhenReady
            currentWindow = player.currentWindowIndex
            playbackPosition = player.currentPosition
        }
//        viewModel.setVideoFormat(false, it.item.itag)
    }, SELECTION_DIFF_CALLBACK)

    private val extraPlayerOptions = listOf(
        PlayerOption("resolution", R.drawable.ic_bookmark_border_white),
        PlayerOption("audio quality", R.drawable.ic_arrow_drop_down_white),
        PlayerOption("online mode", R.drawable.ic_arrow_drop_down_white, true)
    )

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

        viewModel.isOnlineMode.observe(this, Observer {
            runOnUiThread {
                // prevent call notifyItemChanged before recycler view finished layout
                extraPlayerOptions[2].checked = it
                mOptionAdapter.notifyItemChanged(2)
            }
        })
        viewModel.requestOnlineMode.observe(this, Observer { msg ->
//            AlertDialog.Builder(this).apply {
//                setMessage(msg)
//                setPositiveButton("ok") { _, _ -> viewModel.setOnlineMode(this@MainActivity, true) }
//                setNegativeButton("cancel") { _, _ -> }
//                create()
//            }.show()
        })

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

        viewModel.videoMeta.observe(this, Observer {
            mPlayerLayout.setTitle(it.title)
            setBookmarkButton(it.bookmarked)
            val itags = it.itags
            mVtagList = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
                itags.contains(it.key)
            })
            mAtagList = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
                itags.contains(it.key)
            })
            mResOptionDialog = AlertDialog.Builder(this).apply {
                mResOptionDialogView =
                    layoutInflater.inflate(R.layout.dialog_achive_option, null, false)
                mResOptionDialogView!!.apply {
                    spinner_video_quality.apply {
                        adapter = ArrayAdapter<String>(this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            mVtagList.map { it.value.resolution }
                        )
                    }
                    spinner_audio_quality.apply {
                        adapter = ArrayAdapter<String>(this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            mAtagList.map { it.value.bitrate }
                        )
                    }
                }
                setView(mResOptionDialogView)
                setTitle("Select quality")
                setPositiveButton("ok") { _, _ ->
                    // save playback state
                    exoPlayer?.let {
                        playWhenReady = it.playWhenReady
                        currentWindow = it.currentWindowIndex
                        playbackPosition = it.currentPosition
                    }
                    val vKey =
                        mVtagList.map { it.key }[mResOptionDialogView!!.spinner_video_quality.selectedItemPosition]
                    val vTag = mVtagList[vKey]?.itag
                    val aKey =
                        mAtagList.map { it.key }[mResOptionDialogView!!.spinner_audio_quality.selectedItemPosition]
                    val aTag = mAtagList[aKey]?.itag
                    viewModel.setQuality(vTag, aTag)
                }
                setNegativeButton("cancel") { _, _ -> }
            }.create()
            mArchiveOptionDialog = AlertDialog.Builder(this).apply {
                mArchiveOptionDialogView =
                    layoutInflater.inflate(R.layout.dialog_achive_option, null, false)
                mArchiveOptionDialogView!!.apply {
                    spinner_video_quality.apply {
                        adapter = ArrayAdapter<String>(this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            mVtagList.map { it.value.resolution }
                        )
                    }
                    spinner_audio_quality.apply {
                        adapter = ArrayAdapter<String>(this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            mAtagList.map { it.value.bitrate }
                        )
                    }
                }
                setView(mArchiveOptionDialogView)
                setTitle("Archive options")
                setPositiveButton("ok") { _, _ ->
                    lifecycleScope.launch {
                        val vKey =
                            mVtagList.map { it.key }[mArchiveOptionDialogView!!.spinner_video_quality.selectedItemPosition]
                        val vTag = mVtagList[vKey]?.itag
                        val aKey =
                            mAtagList.map { it.key }[mArchiveOptionDialogView!!.spinner_audio_quality.selectedItemPosition]
                        val aTag = mAtagList[aKey]?.itag
                        viewModel.archiveVideo(this@MainActivity, vTag, aTag).let { result ->
                            when (result.status) {
                                Status.SUCCESS -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "has new download task: ${result.data!!}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                Status.ERROR -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        result.stringMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }
                setNegativeButton("cancel") { _, _ -> }
            }.create()
        })

        viewModel.selectedTags.observe(this, Observer {
            val vPos = mVtagList.keys.toList().indexOf(it.vTag)
            val aPos = mAtagList.keys.toList().indexOf(it.aTag)
            mResOptionDialogView?.apply {
                if (vPos >= 0) spinner_video_quality.setSelection(vPos)
                if (aPos >= 0) spinner_audio_quality.setSelection(aPos)
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

//        viewModel.availableITag.observe(this, Observer { itags ->
//            availableVFormats =
//                LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter {
//                    itags.contains(it.key)
//                })
//            availableAFormats =
//                LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter {
//                    itags.contains(it.key)
//                })
//            mResOptionDialog = AlertDialog.Builder(this).apply {
//                val contentView = layoutInflater.inflate(R.layout.dialog_achive_option, null, false)
//                contentView.apply {
//                    spinner_video_quality.apply {
//                        adapter = ArrayAdapter<String>(this@MainActivity,
//                            android.R.layout.simple_spinner_dropdown_item,
//                            availableVFormats.map { it.value.resolution }
//                        )
//                    }
//                    spinner_audio_quality.apply {
//                        adapter = ArrayAdapter<String>(this@MainActivity,
//                            android.R.layout.simple_spinner_dropdown_item,
//                            availableAFormats.map { it.value.bitrate }
//                        )
//                    }
//                }
//                setView(contentView)
//                setTitle("Select quality")
//                setPositiveButton("ok") { _, _ ->
//                    // save playback state
//                    exoPlayer?.let {
//                        playWhenReady = it.playWhenReady
//                        currentWindow = it.currentWindowIndex
//                        playbackPosition = it.currentPosition
//                    }
//                    val vKey = availableVFormats.map { it.key }[contentView.spinner_video_quality.selectedItemPosition]
//                    val vTag = availableVFormats[vKey]?.itag
//                    val aKey = availableAFormats.map { it.key }[contentView.spinner_audio_quality.selectedItemPosition]
//                    val aTag = availableAFormats[aKey]?.itag
//                    viewModel.setQuality(vTag, aTag)
//                }
//                setNegativeButton("cancel") { _, _ -> }
//            }.create()
//            mArchiveOptionDialog = AlertDialog.Builder(this).apply {
//                val contentView = layoutInflater.inflate(R.layout.dialog_achive_option, null, false)
//                contentView.apply {
//                    spinner_video_quality.apply {
//                        adapter = ArrayAdapter<String>(this@MainActivity,
//                            android.R.layout.simple_spinner_dropdown_item,
//                            availableVFormats.map { it.value.resolution }
//                        )
//                    }
//                    spinner_audio_quality.apply {
//                        adapter = ArrayAdapter<String>(this@MainActivity,
//                            android.R.layout.simple_spinner_dropdown_item,
//                            availableAFormats.map { it.value.bitrate }
//                        )
//                    }
//                }
//                setView(contentView)
//                setTitle("Archive options")
//                setPositiveButton("ok") { _, _ ->
//                    lifecycleScope.launch {
//                        val vKey = availableVFormats.map { it.key }[contentView.spinner_video_quality.selectedItemPosition]
//                        val vTag = availableVFormats[vKey]?.itag
//                        val aKey = availableAFormats.map { it.key }[contentView.spinner_audio_quality.selectedItemPosition]
//                        val aTag = availableAFormats[aKey]?.itag
//                        viewModel.archiveVideo(this@MainActivity, vTag, aTag).let { result ->
//                            when (result.status) {
//                                Status.SUCCESS -> {
//                                    Toast.makeText(this@MainActivity,
//                                        "has new download task: ${result.data!!}",
//                                        Toast.LENGTH_SHORT).show()
//                                }
//                                Status.ERROR -> {
//                                    Toast.makeText(this@MainActivity,
//                                        result.stringMessage,
//                                        Toast.LENGTH_SHORT).show()
//                                }
//                                else -> {}
//                            }
//                        }
//                    }
//                }
//                setNegativeButton("cancel") { _, _ -> }
//            }.create()
//        })

        viewModel.videoResSelection.observe(this, Observer {
            mResSelectionAdapter.apply { submitList(it) }
        })

        viewModel.audioQualitySelection.observe(this, Observer {
            mAudioSelectionAdapter.apply { submitList(it) }
        })

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

        Log.d("MOMO", "config change")

        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                mPlayerLayout.setEnableTransition(!mPlayerLayout.isFullscreen)
                if (mPlayerLayout.isSmall) {
                    mPlayerLayout.setPlayerSize(VideoPlayerLayout.PlayerSize.FULLSCREEN)
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                mPlayerLayout.setEnableTransition(true)
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
                        if (allowPlayUsingWifiOnly && !viewModel.isLocalBuffering && !isWifiConnected) return false
                        return super.shouldContinueLoading(bufferedDurationUs, playbackSpeed)
                    }
                })
            mPlayerLayout.setPlayer(exoPlayer)
            exoPlayer?.let { player ->
                player.playWhenReady = playWhenReady
                player.seekTo(currentWindow, playbackPosition)
            }
        }
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            playWhenReady = exoPlayer!!.playWhenReady
            playbackPosition = exoPlayer!!.currentPosition
            currentWindow = exoPlayer!!.currentWindowIndex
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
                        R.id.archive -> {
                            viewModel.videoMeta.value?.videoId?.let { vid ->
                                showArchiveOption(vid)
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
                    if (start == VideoPlayerLayout.PlayerSize.DISMISS && end == VideoPlayerLayout.PlayerSize.FULLSCREEN) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                    }
                }

                @SuppressLint("SourceLockedOrientationActivity")
                override fun onComplete(current: VideoPlayerLayout.PlayerSize) {
                    // when there is transition, check if player size is fullscreen,
                    // if true then disable user transition when orientation is not portrait
                    when (current) {
                        VideoPlayerLayout.PlayerSize.FULLSCREEN -> {
                            setEnableTransition(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                        }
                        VideoPlayerLayout.PlayerSize.SMALL -> {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        }
                        VideoPlayerLayout.PlayerSize.DISMISS -> {
                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
        mResolutionDialog = PlayerBottomSheetDialogBuilder.createDialog(this) {
            it.recycler_view.apply {
                mResolutionRv = this
                adapter = mResSelectionAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
        }
        mAudioQualityDialog = PlayerBottomSheetDialogBuilder.createDialog(this) {
            it.recycler_view.apply {
                mQualityRv = this
                adapter = mAudioSelectionAdapter
                layoutManager = LinearLayoutManager(this@MainActivity)
            }
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
