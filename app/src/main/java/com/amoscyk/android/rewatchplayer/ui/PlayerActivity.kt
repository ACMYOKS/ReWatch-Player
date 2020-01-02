package com.amoscyk.android.rewatchplayer.ui

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.amoscyk.android.rewatchplayer.R
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private var videoId: String = ""
    private lateinit var playerView: PlayerView
    private var exoPlayer: SimpleExoPlayer? = null

    private var playWhenReady = false
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoId = intent.extras?.getString(VIDEO_ID_KEY) ?: ""

        setupViews()

        confirm_btn.setOnClickListener {
            val str = input_field.text.toString()
            prepareVideoResource(str)
        }
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
        playerView = findViewById(R.id.player_view)
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this)
            playerView.player = exoPlayer
            exoPlayer?.apply {
                playWhenReady = true
                seekTo(currentWindow, playbackPosition)
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

    private fun prepareVideoResource(videoId: String) {
        GlobalScope.launch {
            com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor().extractInfo(videoId)
        }
        @SuppressLint("StaticFieldLeak")
        val extractor = object : YouTubeExtractor(this) {
            override fun onExtractionComplete(
                ytFiles: SparseArray<YtFile>?,
                videoMeta: VideoMeta?
            ) {
                ytFiles?.let {
                    Log.d("TAG", it.get(22).url)
                    it.get(22)?.url?.let { url ->
                        exoPlayer?.prepare(buildMediaResource(url))
                    }
                }
            }
        }
        extractor.extract(videoId, true, true)
    }

    private fun buildMediaResource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "ReWatch Player")
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }

    private fun buildMediaResource(url: String): MediaSource {
        val uri = Uri.parse(url)
        return buildMediaResource(uri)
    }

    companion object {
        const val VIDEO_ID_KEY: String = "videoId"
        const val YT_URL = "https://www.youtube.com/watch?v="
    }
}
