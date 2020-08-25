package com.amoscyk.android.rewatchplayer.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amoscyk.android.rewatchplayer.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import java.util.*

class AudioPlayerService : Service() {

    private var exoPlayer: ExoPlayer? = null
    private val defaultFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(this, getString(R.string.app_name))
    }
    private val progressiveSrcFactory: ProgressiveMediaSource.Factory by lazy {
        ProgressiveMediaSource.Factory(defaultFactory)
    }
    private var notiMngr: PlayerNotificationManager? = null

    private var videoId: String? = null
    private var title: String? = null
    private var content: String? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this)
        notiMngr = PlayerNotificationManager.createWithNotificationChannel(
            this, PLAYBACK_CHANNEL_ID, R.string.player_notification_channel_name, R.string.player_notification_channel_description,
            PLAYBACK_NOTIFICATION_ID, object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    return null
                }

                override fun getCurrentContentText(player: Player?): String? {
                    return content ?: getString(R.string.player_notification_unknown_content)
                }

                override fun getCurrentContentTitle(player: Player?): String {
                    return title ?: getString(R.string.player_notification_unknown_title)
                }

                override fun getCurrentLargeIcon(
                    player: Player?,
                    callback: PlayerNotificationManager.BitmapCallback?
                ): Bitmap? {
                    return null
                }
            }, object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification?,
                    ongoing: Boolean
                ) {
                    startForeground(notificationId, notification!!)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    stopSelf()
                }
            })
        notiMngr!!.apply {
            setPlayer(exoPlayer)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AudioPlayerService", "start service")
        (intent?.getParcelableArrayExtra(EXTRA_KEY_URI_LIST))?.let { extra ->
            if (extra.all { it is Uri }) {
                @Suppress("UNCHECKED_CAST")
                val resUris = extra.map { it as Uri }.toTypedArray()
                val playbackPos = intent.getLongExtra(EXTRA_KEY_PLAYBACK_POSITION, 0)
                videoId = intent.getStringExtra(EXTRA_KEY_VIDEO_ID)
                title = intent.getStringExtra(EXTRA_KEY_VIDEO_TITLE)
                content = intent.getStringExtra(EXTRA_KEY_VIDEO_CONTENT)
                prepareMediaResource(resUris)
                exoPlayer?.playWhenReady = true
                exoPlayer?.seekTo(playbackPos)
                Log.d("AudioPlayerService", "uri: ${resUris.joinToString { it.toString() }}")
                Log.d("AudioPlayerService", "start pos: $playbackPos")
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        sendBroadcast(Intent(ACTION_GET_PLAYBACK_POSITION)
            .putExtra(EXTRA_KEY_PLAYBACK_POSITION, exoPlayer?.currentPosition ?: 0L)
            .putExtra(EXTRA_KEY_VIDEO_ID, videoId))
        notiMngr?.setPlayer(null)
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    private fun prepareMediaResource(uris: Array<Uri>) {
        uris.mapNotNull { progressiveSrcFactory.createMediaSource(it) }.apply {
            when (size) {
                0 -> return
                1 -> exoPlayer?.prepare(first())
                else -> exoPlayer?.prepare(MergingMediaSource(*toTypedArray()))
            }
        }
    }

    class IntentBuilder(val context: Context) {
        private var _videoId: String? = null
        private var _urlList = arrayOf<Uri>()
        private var _playbackPos = 0L
        private var _title: String? = null
        private var _content: String? = null

        fun setVideoId(videoId: String): IntentBuilder {
            _videoId = videoId
            return this
        }
        fun setUriList(urlList: Array<Uri>): IntentBuilder {
            _urlList = urlList
            return this
        }
        fun setPlaybackPosition(playbackPosition: Long): IntentBuilder {
            _playbackPos = playbackPosition
            return this
        }
        fun setTitle(title: String): IntentBuilder {
            _title = title
            return this
        }
        fun setContent(content: String): IntentBuilder {
            _content = content
            return this
        }
        fun build(): Intent {
            return Intent(context, AudioPlayerService::class.java)
                .putExtra(EXTRA_KEY_VIDEO_ID, _videoId)
                .putExtra(EXTRA_KEY_URI_LIST, _urlList)
                .putExtra(EXTRA_KEY_PLAYBACK_POSITION, _playbackPos)
                .putExtra(EXTRA_KEY_VIDEO_TITLE, _title)
                .putExtra(EXTRA_KEY_VIDEO_CONTENT, _content)
        }
    }

    companion object {
        const val EXTRA_KEY_URI_LIST = "com.amoscyk.android.rewatchplayer.service.URI_LIST"
        const val EXTRA_KEY_PLAYBACK_POSITION = "com.amoscyk.android.rewatchplayer.service.PLAYBACK_POSITION"
        const val EXTRA_KEY_VIDEO_ID = "com.amoscyk.android.rewatchplayer.service.VIDEO_ID"
        const val EXTRA_KEY_VIDEO_TITLE = "com.amoscyk.android.rewatchplayer.service.VIDEO_TITLE"
        const val EXTRA_KEY_VIDEO_CONTENT = "com.amoscyk.android.rewatchplayer.service.VIDEO_CONTENT"
        const val ACTION_GET_PLAYBACK_POSITION = "com.amoscyk.android.rewatchplayer.service.ACTION_GET_PLAYBACK_POSITION"
        const val PLAYBACK_CHANNEL_ID = "playbackChannelId"
        const val PLAYBACK_NOTIFICATION_ID = 1
    }

}
