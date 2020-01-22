package com.amoscyk.android.rewatchplayer.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.rpApplication
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.ytextractor.YouTubeExtractor
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private var videoId: String? = null
    private lateinit var mPlayerView: PlayerView
    private var exoPlayer: SimpleExoPlayer? = null

    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var currentWindow: Int = 0

    private val videoUrl = MutableLiveData<String>()
    private val adaptiveUrls = MutableLiveData<Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        videoId = intent.extras?.getString(EXTRA_VIDEO_ID)

        setupViews()

        videoUrl.observe(this, Observer {
//            exoPlayer?.prepare(buildMediaResource(it))
        })

        adaptiveUrls.observe(this, Observer {
            val f = DefaultDataSourceFactory(this, "ReWatch Player")
            val v = ProgressiveMediaSource.Factory(f).createMediaSource(Uri.parse(it.first))
            val a = ProgressiveMediaSource.Factory(f).createMediaSource(Uri.parse(it.second))
            exoPlayer?.prepare(MergingMediaSource(a,v))
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
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this)
            mPlayerView.player = exoPlayer
            exoPlayer?.apply {
                playWhenReady = this@PlayerActivity.playWhenReady
                seekTo(this@PlayerActivity.currentWindow, this@PlayerActivity.playbackPosition)
            }
            videoId?.let {
                prepareVideoResource(it)
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
            val info = YouTubeExtractor(rpApplication.youtubeOpenService).extractInfo(videoId)
            if (info != null) {
                if (info.urlMap.containsKey(299) && info.urlMap.containsKey(140)) {
                    adaptiveUrls.postValue(Pair(info.urlMap.getValue(299), info.urlMap.getValue(140)))
                }
                val a = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
                val v = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
                val av = arrayListOf<YouTubeStreamFormatCode.StreamFormat>()
                info.availableFormats.asSequence().filter {
                    it.container == YouTubeStreamFormatCode.Container.MP4
                }.apply {
                    filterTo(a) { it.content == YouTubeStreamFormatCode.Content.A }
                    filterTo(v) { it.content == YouTubeStreamFormatCode.Content.V }
                    filterTo(av) { it.content == YouTubeStreamFormatCode.Content.AV }
                }
                if (info.urlMap.containsKey(299)) {
                    val url = info.urlMap[299]
                    if (url != null) {
                        videoUrl.postValue(url)
                    }
                }
            }
        }
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
        const val EXTRA_VIDEO_ID: String = "com.amoscyk.android.rewatchplayer.ui.PlayerActivity.extra.videoId"
        const val YT_URL = "https://www.youtube.com/watch?v="
    }
}
