package com.amoscyk.android.rewatchplayer.ui.player

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_dialog_archive_option.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog_archive_option.view.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog_archive_option.view.confirm_btn
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import java.io.File

class PlayerActivity : ReWatchPlayerActivity() {

    private var videoId: String? = null
    private var videoInfo: RPVideo? = null
    private lateinit var mPlayerView: PlayerView
    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mToolbar: Toolbar
    private lateinit var mTitleTv: TextView
    private lateinit var mResSpinner: AppCompatSpinner
    private lateinit var mOptionDialog: BottomSheetDialog
    private lateinit var mResolutionDialog: BottomSheetDialog
    private lateinit var mAudioQualityDialog: BottomSheetDialog
    private lateinit var mArchiveOptionDialog: BottomSheetDialog
    private lateinit var mResolutionRv: RecyclerView
    private var exoPlayer: SimpleExoPlayer? = null

    private val mResSelectionAdapter = PlayerSelectionAdapter({
        mResolutionDialog.dismiss()
        viewModel.setVideoFormat(it.item.itag)
        exoPlayer?.let { player ->
            // save playback state
            playWhenReady = player.playWhenReady
            currentWindow = player.currentWindowIndex
            playbackPosition = player.currentPosition
        }
    }, object :
        DiffUtil.ItemCallback<PlayerSelection<PlayerViewModel.VideoResSelection>>() {
        override fun areItemsTheSame(
            oldItem: PlayerSelection<PlayerViewModel.VideoResSelection>,
            newItem: PlayerSelection<PlayerViewModel.VideoResSelection>
        ) = oldItem.item == newItem.item && oldItem.selected == newItem.selected

        override fun areContentsTheSame(
            oldItem: PlayerSelection<PlayerViewModel.VideoResSelection>,
            newItem: PlayerSelection<PlayerViewModel.VideoResSelection>
        ) = oldItem == newItem
    })
    private lateinit var mArchiveVideoOptionSpinner: AppCompatSpinner
    private lateinit var mArchiveAudioOptionSpinner: AppCompatSpinner
    private var selectedVideoArchiveTag: Int? = null
    private var selectedAudioArchiveTag: Int? = null

    private val extraPlayerOptions = listOf(
        PlayerOption("resolution", R.drawable.ic_bookmark_border_white),
        PlayerOption("audio quality", R.drawable.ic_arrow_drop_down_white),
        PlayerOption("test item 1", R.drawable.ic_arrow_drop_down_white),
        PlayerOption("test item 233333", R.drawable.ic_arrow_drop_down_white),
        PlayerOption("test item 34444444", R.drawable.ic_arrow_drop_down_white)
    )

    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0

    private val defaultFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(this, getString(R.string.app_name))
    }
    private val progressiveSrcFactory: ProgressiveMediaSource.Factory by lazy {
        ProgressiveMediaSource.Factory(defaultFactory)
    }

    private val viewModel by viewModels<PlayerViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        videoInfo = intent.getParcelableExtra(EXTRA_VIDEO_INFO)

        setupViews()

        viewModel.videoInfo.observe(this, Observer {
            mTitleTv.text = it.title
        })

        viewModel.videoUrl.observe(this, Observer {
            val v = progressiveSrcFactory.createMediaSource(Uri.parse(it))
            exoPlayer?.prepare(v)
            exoPlayer?.playWhenReady = playWhenReady
            exoPlayer?.seekTo(currentWindow, playbackPosition)
        })

        viewModel.adaptiveUrls.observe(this, Observer {
            val v = progressiveSrcFactory.createMediaSource(Uri.parse(it.videoUrl))
            val a = progressiveSrcFactory.createMediaSource(Uri.parse(it.audioUrl))
            exoPlayer?.prepare(MergingMediaSource(a,v))
            exoPlayer?.playWhenReady = playWhenReady
            exoPlayer?.seekTo(currentWindow, playbackPosition)
        })

        viewModel.availableStream.observe(this, Observer { formats ->
            mArchiveVideoOptionSpinner.apply {
                val keyList = formats.videoStreamFormat.keys.toList()
                adapter = ArrayAdapter<String>(this@PlayerActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    keyList.map { formats.videoStreamFormat.getValue(it).resolution }
                )
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                                id: Long) {
                        Log.d(TAG, "selected video itag: ${keyList[position]}")
                        selectedVideoArchiveTag = keyList[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedVideoArchiveTag = null
                    }
                }
            }
            mArchiveAudioOptionSpinner.apply {
                val keyList = formats.audioStreamFormat.keys.toList()
                adapter = ArrayAdapter<String>(this@PlayerActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    keyList.map { formats.audioStreamFormat.getValue(it).bitrate })
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                                id: Long) {
                        Log.d(TAG, "selected audio itag: ${keyList[position]}")
                        selectedAudioArchiveTag = keyList[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedAudioArchiveTag = null
                    }
                }
            }
        })

        viewModel.videoResSelection.observe(this, Observer {
            mResSelectionAdapter.apply {
                submitList(it)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (exoPlayer == null) {
            initPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun setupViews() {
        mPlayerView = findViewById(R.id.player_view)
        mAppBarLayout = findViewById(R.id.app_bar_layout)
        mToolbar = findViewById(R.id.toolbar)
        mTitleTv = findViewById(R.id.video_title_tv)
        mResSpinner = findViewById(R.id.resolution_spinner)
        mOptionDialog = BottomSheetDialog(this).apply {
            val contentView = layoutInflater.inflate(R.layout.bottom_sheet_dialog_user_option, null, false)
            setContentView(contentView)
            contentView.recycler_view.apply {
                adapter = PlayerOptionAdapter { option, position ->
                    if (position == 0) {
                        mOptionDialog.dismiss()
                        mResolutionDialog.show()
                    }
                }.apply { submitList(extraPlayerOptions) }
                layoutManager = LinearLayoutManager(this@PlayerActivity)
            }
        }
        mResolutionDialog = BottomSheetDialog(this).apply {
            val contentView = layoutInflater.inflate(R.layout.bottom_sheet_dialog_user_option, null, false)
            setContentView(contentView)
            contentView.recycler_view.apply {
                mResolutionRv = this
                adapter = mResSelectionAdapter
                layoutManager = LinearLayoutManager(this@PlayerActivity)
            }
        }
        mArchiveOptionDialog = BottomSheetDialog(this).apply {
            val contentView = layoutInflater.inflate(R.layout.bottom_sheet_dialog_archive_option, null, false)
            setContentView(contentView)
            mArchiveVideoOptionSpinner = contentView.video_option_spinner
            mArchiveAudioOptionSpinner = contentView.audio_option_spinner
            confirm_btn.setOnClickListener {
                if (selectedAudioArchiveTag != null && selectedVideoArchiveTag != null) {
                    viewModel.archiveVideo(this@PlayerActivity, selectedVideoArchiveTag!!, selectedAudioArchiveTag!!)
                }
            }
        }
        mToolbar.apply {
            setNavigationOnClickListener {
            // FIXME: home button may have different behavior with back button
            onBackPressed()
        }
            inflateMenu(R.menu.player_option_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.bookmark -> {

                    }
                    R.id.remove_bookmark -> {

                    }
                    R.id.archive -> {
                        mArchiveOptionDialog.show()
                    }
                    R.id.other_action -> {
                        mOptionDialog.show()
                    }
                }
                true
            }
        }
        mPlayerView.setControllerVisibilityListener { visibility ->
            mAppBarLayout.visibility = visibility
        }
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this)
            mPlayerView.player = exoPlayer
            exoPlayer?.let { player ->
                player.playWhenReady = playWhenReady
                player.seekTo(currentWindow, playbackPosition)
            }
            videoId?.let {
                viewModel.prepareVideoResource(it)
            }
            videoInfo?.let {
                viewModel.prepareVideoResource(it.id)
            }
//            val file = File(this.getExternalFilesDir("pb.res"), "FMo4Qarn0tY-160.mp4")
//            Log.d("XYZ", "${file.absolutePath}, ${file.exists()}")
//            val v = progressiveSrcFactory.createMediaSource(file.toUri())
//            exoPlayer?.prepare(v)
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

    companion object {
        const val TAG = "PlayerActivity"
        const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity.extra.videoId"
        const val EXTRA_VIDEO_INFO = "com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity.extra.videoInfo"
        const val YT_URL = "https://www.youtube.com/watch?v="
    }
}
