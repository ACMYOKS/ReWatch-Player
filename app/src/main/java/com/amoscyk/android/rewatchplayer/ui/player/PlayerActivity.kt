package com.amoscyk.android.rewatchplayer.ui.player

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.*
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_dialog_user_option.view.*
import kotlinx.android.synthetic.main.dialog_archive_option.view.*
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
    private lateinit var mQualityRv: RecyclerView
    private var exoPlayer: SimpleExoPlayer? = null

    private var availableVFormats = linkedMapOf<Int, YouTubeStreamFormatCode.StreamFormat>()
    private var availableAFormats = linkedMapOf<Int, YouTubeStreamFormatCode.StreamFormat>()

    private val mOptionAdapter = PlayerOptionAdapter({ option, position ->
        if (position == 0) {
            mResolutionDialog.show()
            mOptionDialog.dismiss()
        } else if (position == 1) {
            mAudioQualityDialog.show()
            mOptionDialog.dismiss()
        }
    }, { isChecked, position ->
        if (position == 2) {
            viewModel.setOnlineMode(this, isChecked)
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
        viewModel.setVideoFormat(true, it.item.itag)

    }, SELECTION_DIFF_CALLBACK)
    private val mAudioSelectionAdapter = PlayerSelectionAdapter({
        mAudioQualityDialog.dismiss()
        exoPlayer?.let { player ->
            // save playback state
            playWhenReady = player.playWhenReady
            currentWindow = player.currentWindowIndex
            playbackPosition = player.currentPosition
        }
        viewModel.setVideoFormat(false, it.item.itag)
    }, SELECTION_DIFF_CALLBACK)
//    private lateinit var mArchiveVideoOptionSpinner: AppCompatSpinner
//    private lateinit var mArchiveAudioOptionSpinner: AppCompatSpinner
//    private var selectedVideoArchiveTag: Int? = null
//    private var selectedAudioArchiveTag: Int? = null

    private val extraPlayerOptions = listOf(
        PlayerOption("resolution", R.drawable.ic_bookmark_border_white),
        PlayerOption("audio quality", R.drawable.ic_arrow_drop_down_white),
        PlayerOption("online mode", R.drawable.ic_arrow_drop_down_white, true),
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

        viewModel.isOnlineMode.observe(this, Observer {
            runOnUiThread {
                // prevent call notifyItemChanged before recycler view finished layout
                extraPlayerOptions[2].checked = it
                mOptionAdapter.notifyItemChanged(2)
            }
        })

        viewModel.requestOnlineMode.observe(this, Observer { msg ->
            AlertDialog.Builder(this).apply {
                setMessage(msg)
                setPositiveButton("ok") { _, _ -> viewModel.setOnlineMode(this@PlayerActivity, true) }
                setNegativeButton("cancel") { _, _ -> }
                create()
            }.show()
        })

        viewModel.videoMeta.observe(this, Observer {
            mTitleTv.text = it.title
//            setBookmarkButton(it.bookmarked)
        })

        viewModel.resourceUrl.observe(this, Observer {
            prepareMediaResource(
                it.videoUrl?.let { url -> Uri.parse(url) },
                it.audioUrl?.let { url -> Uri.parse(url) }
            )
        })

        viewModel.resourceFile.observe(this, Observer {
            prepareMediaResource(
                it.videoFile?.let { filename -> getResFileUriIfExist(filename) },
                it.audioFile?.let { filename -> getResFileUriIfExist(filename) }
            )
        })

        viewModel.availableITag.observe(this, Observer { itags ->
            availableVFormats = LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter { itags.contains(it.key) })
            availableAFormats =  LinkedHashMap(YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter { itags.contains(it.key) })
//            mArchiveVideoOptionSpinner.apply {
//                val res = YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filter { itags.contains(it.key) }
//                adapter = ArrayAdapter<String>(this@PlayerActivity,
//                    android.R.layout.simple_spinner_dropdown_item,
//                    res.map { it.value.resolution }
//                )
//                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
//                                                id: Long) {
//                        Log.d(TAG, "selected video itag: ${res[position]?.itag}")
//                        selectedVideoArchiveTag = res[position]?.itag
//                    }
//
//                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        selectedVideoArchiveTag = null
//                    }
//                }
//            }
//            mArchiveAudioOptionSpinner.apply {
//                val res = YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filter { itags.contains(it.key) }
//                adapter = ArrayAdapter<String>(this@PlayerActivity,
//                    android.R.layout.simple_spinner_dropdown_item,
//                    res.map { it.value.bitrate }
//                )
//                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
//                                                id: Long) {
//                        Log.d(TAG, "selected video itag: ${res[position]?.itag}")
//                        selectedAudioArchiveTag = res[position]?.itag
//                    }
//
//                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        selectedAudioArchiveTag = null
//                    }
//                }
//            }
        })

//        viewModel.availableStream.observe(this, Observer { formats ->
//            mArchiveVideoOptionSpinner.apply {
//                val keyList = formats.videoStreamFormat.keys.toList()
//                adapter = ArrayAdapter<String>(this@PlayerActivity,
//                    android.R.layout.simple_spinner_dropdown_item,
//                    keyList.map { formats.videoStreamFormat.getValue(it).resolution }
//                )
//                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
//                                                id: Long) {
//                        Log.d(TAG, "selected video itag: ${keyList[position]}")
//                        selectedVideoArchiveTag = keyList[position]
//                    }
//
//                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        selectedVideoArchiveTag = null
//                    }
//                }
//            }
//            mArchiveAudioOptionSpinner.apply {
//                val keyList = formats.audioStreamFormat.keys.toList()
//                adapter = ArrayAdapter<String>(this@PlayerActivity,
//                    android.R.layout.simple_spinner_dropdown_item,
//                    keyList.map { formats.audioStreamFormat.getValue(it).bitrate })
//                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
//                                                id: Long) {
//                        Log.d(TAG, "selected audio itag: ${keyList[position]}")
//                        selectedAudioArchiveTag = keyList[position]
//                    }
//
//                    override fun onNothingSelected(parent: AdapterView<*>?) {
//                        selectedAudioArchiveTag = null
//                    }
//                }
//            }
//        })

        viewModel.videoResSelection.observe(this, Observer {
            mResSelectionAdapter.apply { submitList(it) }
        })

        viewModel.audioQualitySelection.observe(this, Observer {
            mAudioSelectionAdapter.apply { submitList(it) }
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
                adapter = mOptionAdapter.apply { submitList(extraPlayerOptions) }
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
        mAudioQualityDialog = BottomSheetDialog(this).apply {
            val contentView = layoutInflater.inflate(R.layout.bottom_sheet_dialog_user_option, null, false)
            setContentView(contentView)
            contentView.recycler_view.apply {
                mQualityRv = this
                adapter = mAudioSelectionAdapter
                layoutManager = LinearLayoutManager(this@PlayerActivity)
            }
        }
//        mArchiveOptionDialog = BottomSheetDialog(this).apply {
//            val contentView = layoutInflater.inflate(R.layout.bottom_sheet_dialog_archive_option, null, false)
//            setContentView(contentView)
//            mArchiveVideoOptionSpinner = contentView.video_option_spinner
//            mArchiveAudioOptionSpinner = contentView.audio_option_spinner
//            confirm_btn.setOnClickListener {
//                if (selectedAudioArchiveTag != null && selectedVideoArchiveTag != null) {
//                    viewModel.archiveVideo(this@PlayerActivity, selectedVideoArchiveTag!!, selectedAudioArchiveTag!!) {
//                        Toast.makeText(this@PlayerActivity, "added archive list: $it", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                dismiss()
//            }
//        }
        mToolbar.apply {
            setNavigationOnClickListener {
                // FIXME: home button may have different behavior with back button
                onBackPressed()
            }
            inflateMenu(R.menu.player_option_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.bookmark -> {
                        viewModel.updateBookmarkStatus(true)
                    }
                    R.id.remove_bookmark -> {
                        viewModel.updateBookmarkStatus(false)
                    }
//                    R.id.archive -> {
//                        mArchiveOptionDialog.show()
//                        showArchiveOptions()
//                    }
                    R.id.other_action -> {
                        mOptionDialog.show()
                    }
                }
                true
            }
            setBookmarkButton(false)        // set visibility before view data is loaded
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
//                viewModel.prepareLocalVideoResource(this, it)
                viewModel.prepareVideoResource(this, it)
            }
            videoInfo?.let {
//                viewModel.prepareLocalVideoResource(this, it.id)
                viewModel.prepareVideoResource(this, it.id)
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

    private fun prepareMediaResource(videoUri: Uri?, audioUri: Uri?) {
        val resList = arrayListOf<ProgressiveMediaSource>()
        videoUri?.let { resList.add(progressiveSrcFactory.createMediaSource(it)) }
        audioUri?.let { resList.add(progressiveSrcFactory.createMediaSource(it)) }
        when (resList.size) {
            0 -> return
            1 -> exoPlayer?.prepare(resList[0])
            else -> exoPlayer?.prepare(MergingMediaSource(*resList.toTypedArray()))
        }
        exoPlayer?.playWhenReady = playWhenReady
        exoPlayer?.seekTo(currentWindow, playbackPosition)
    }

    private fun getResFileUriIfExist(filename: String): Uri? {
        val file = File(FileDownloadHelper.getDir(this), filename)
        return if (file.exists()) file.toUri() else null
    }

    private fun setBookmarkButton(isBookmarked: Boolean) {
        val btnBookmark = mToolbar.menu.findItem(R.id.bookmark)
        val btnRemoveBookmark = mToolbar.menu.findItem(R.id.remove_bookmark)
        btnBookmark.isVisible = !isBookmarked
        btnRemoveBookmark.isVisible = isBookmarked
    }

    private fun showArchiveOptions() {
        AlertDialog.Builder(this).apply {
            val contentView = layoutInflater.inflate(R.layout.dialog_archive_option, null, false)
            contentView.apply {
                spinner_video_quality.apply {
                    adapter = ArrayAdapter<String>(this@PlayerActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        availableVFormats.map { it.value.resolution }
                    )
                }
                spinner_audio_quality.apply {
                    adapter = ArrayAdapter<String>(this@PlayerActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        availableAFormats.map { it.value.bitrate }
                    )
                }
            }
            setView(contentView)
            setTitle("Archive options")
            setPositiveButton("ok") { _, _ ->
                val vKey = availableVFormats.map { it.key }[contentView.spinner_video_quality.selectedItemPosition]
                val vTag = availableVFormats[vKey]?.itag
                val aKey = availableAFormats.map { it.key }[contentView.spinner_audio_quality.selectedItemPosition]
                val aTag = availableAFormats[aKey]?.itag
                viewModel.archiveVideo(this@PlayerActivity, vTag, aTag) {
                    Toast.makeText(this@PlayerActivity, "has new download task: $it", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("cancel") { _, _ -> }
            create()
        }.show()
    }

    companion object {
        const val TAG = "PlayerActivity"
        const val EXTRA_VIDEO_ID = "com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity.extra.videoId"
        const val EXTRA_VIDEO_INFO = "com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity.extra.videoInfo"
        const val YT_URL = "https://www.youtube.com/watch?v="

        private val SELECTION_DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<PlayerSelection<PlayerViewModel.VideoQualitySelection>>() {
            override fun areItemsTheSame(
                oldItem: PlayerSelection<PlayerViewModel.VideoQualitySelection>,
                newItem: PlayerSelection<PlayerViewModel.VideoQualitySelection>
            ) = oldItem.item == newItem.item && oldItem.selected == newItem.selected

            override fun areContentsTheSame(
                oldItem: PlayerSelection<PlayerViewModel.VideoQualitySelection>,
                newItem: PlayerSelection<PlayerViewModel.VideoQualitySelection>
            ) = oldItem == newItem
        }
    }
}
