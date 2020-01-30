package com.amoscyk.android.rewatchplayer.ui.player

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.rpApplication
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.appbar.AppBarLayout

class PlayerActivity : ReWatchPlayerActivity() {

    private var videoId: String? = null
    private lateinit var mPlayerView: PlayerView
    private lateinit var mAppBarLayout: AppBarLayout
    private lateinit var mToolbar: Toolbar
    private lateinit var mResSpinner: AppCompatSpinner
    private var exoPlayer: SimpleExoPlayer? = null

    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0

    private val defaultFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(this, "ReWatch Player")
    }
    private val progressiveSrcFactory: ProgressiveMediaSource.Factory by lazy {
        ProgressiveMediaSource.Factory(defaultFactory)
    }

    private val viewModel by viewModels<PlayerViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoId = intent.extras?.getString(EXTRA_VIDEO_ID)

        setupViews()

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

        viewModel.availableStream.observe(this, Observer {

        })

        viewModel.availableVideoResolution.observe(this, Observer { formats ->
            mResSpinner.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, formats.map { it.resolution })
            mResSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedRes = formats[position]
                    viewModel.setVideoFormat(selectedRes.itag)
                    exoPlayer?.let { player ->
                        // save playback state
                        playWhenReady = player.playWhenReady
                        currentWindow = player.currentWindowIndex
                        playbackPosition = player.currentPosition
                    }
                    Toast.makeText(this@PlayerActivity, selectedRes.itag.toString(), Toast.LENGTH_SHORT).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        })

        viewModel.selectedVideoResolution.observe(this, Observer { selectedRes ->
            viewModel.availableVideoResolution.value?.indexOf(selectedRes)?.let {
                mResSpinner.setSelection(it)
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
        mResSpinner = findViewById(R.id.resolution_spinner)
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

//    private fun prepareVideoResource(videoId: String) {
//        GlobalScope.launch {
//            val info = YouTubeExtractor(rpApplication.youtubeOpenService).extractInfo(videoId)
//            if (info != null) {
//                if (info.urlMap.containsKey(299) && info.urlMap.containsKey(140)) {
//                    adaptiveUrls.postValue(Pair(info.urlMap.getValue(299), info.urlMap.getValue(140)))
//                }
//                val a = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
//                val v = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
//                val av = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
//                info.muxedStream.asSequence().filter {
//                    it.container == YouTubeStreamFormatCode.Container.MP4
//                }.apply {
//                    filterTo(a) { it.content == YouTubeStreamFormatCode.Content.A }
//                    filterTo(v) { it.content == YouTubeStreamFormatCode.Content.V }
//                    filterTo(av) { it.content == YouTubeStreamFormatCode.Content.AV }
//                }
//                if (info.urlMap.containsKey(22)) {
//                    val url = info.urlMap[22]
//                    if (url != null) {
//                        videoUrl.postValue(url)
//                    }
//                }
//            }
//        }
//    }

    companion object {
        const val EXTRA_VIDEO_ID: String = "com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity.extra.videoId"
        const val YT_URL = "https://www.youtube.com/watch?v="
    }
}
