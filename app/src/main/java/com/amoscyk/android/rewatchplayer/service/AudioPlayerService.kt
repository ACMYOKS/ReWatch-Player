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
import com.amoscyk.android.rewatchplayer.ReWatchPlayerApplication
import com.amoscyk.android.rewatchplayer.ui.MainActivity
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getInt
import com.amoscyk.android.rewatchplayer.util.isMyActivityRunning
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class AudioPlayerService : Service() {

    private val rpApp: ReWatchPlayerApplication
        get() = application as ReWatchPlayerApplication
    private var notiMngr: PlayerNotificationManager? = null

    private var videoId: String? = null
    private var title: String? = null
    private var content: String? = null
    private var vTag: Int? = null
    private var aTag: Int? = null

    private val playerListener = object : Player.EventListener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            // FIXME: cannot dismiss notification sometimes
            if (!isPlaying) stopForeground(false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notiMngr = PlayerNotificationManager.createWithNotificationChannel(
            this, PLAYBACK_CHANNEL_ID, R.string.player_notification_channel_name, R.string.player_notification_channel_description,
            PLAYBACK_NOTIFICATION_ID, object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    val intent = Intent(this@AudioPlayerService, MainActivity::class.java)
                    return PendingIntent.getActivity(this@AudioPlayerService, 0, intent, 0)
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
            setPlayer(rpApp.getPlayer())
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            setSmallIcon(R.drawable.ic_app_icon_outline)
            rpApp.getPlayer()?.addListener(playerListener)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AudioPlayerService", "start service")
        if (intent != null) {
            videoId = intent.getStringExtra(EXTRA_KEY_VIDEO_ID)
            title = intent.getStringExtra(EXTRA_KEY_VIDEO_TITLE)
            content = intent.getStringExtra(EXTRA_KEY_VIDEO_CONTENT)
            vTag = intent.getIntExtra(EXTRA_KEY_VIDEO_ITAG, 0)
            aTag = intent.getIntExtra(EXTRA_KEY_AUDIO_ITAG, 0)
            rpApp.getPlayer()?.playWhenReady = true
            notiMngr?.apply {
                val rewind =
                    rpApp.appSharedPreference.getInt(PreferenceKey.PLAYER_SKIP_BACKWARD_TIME, -1)
                val forward =
                    rpApp.appSharedPreference.getInt(PreferenceKey.PLAYER_SKIP_FORWARD_TIME, -1)
                if (rewind > 0) {
                    setRewindIncrementMs(rewind * 1000L)
                }
                if (forward > 0) {
                    setFastForwardIncrementMs(forward * 1000L)
                }
                invalidate()
            }

        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        sendBroadcast(Intent(ACTION_GET_PLAYBACK_POSITION)
            .putExtra(EXTRA_KEY_PLAYBACK_POSITION, rpApp.getPlayer()?.currentPosition ?: 0L)
            .putExtra(EXTRA_KEY_VIDEO_ID, videoId)
            .putExtra(EXTRA_KEY_VIDEO_ITAG, vTag)
            .putExtra(EXTRA_KEY_AUDIO_ITAG, aTag))
        rpApp.getPlayer()?.removeListener(playerListener)
        notiMngr?.setPlayer(null)
        if (!rpApp.isMyActivityRunning(MainActivity::class.java)) {
            rpApp.releasePlayer()
        }
        super.onDestroy()
    }

    class IntentBuilder(val context: Context) {
        private var _videoId: String? = null
        private var _urlList = arrayOf<Uri>()
        private var _playbackPos = 0L
        private var _title: String? = null
        private var _content: String? = null
        private var _vTag: Int? = null
        private var _aTag: Int? = null

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
        fun setVTag(tag: Int): IntentBuilder {
            _vTag = tag
            return this
        }
        fun setATag(tag: Int): IntentBuilder {
            _aTag = tag
            return this
        }
        fun build(): Intent {
            return Intent(context, AudioPlayerService::class.java)
                .putExtra(EXTRA_KEY_VIDEO_ID, _videoId)
                .putExtra(EXTRA_KEY_URI_LIST, _urlList)
                .putExtra(EXTRA_KEY_PLAYBACK_POSITION, _playbackPos)
                .putExtra(EXTRA_KEY_VIDEO_TITLE, _title)
                .putExtra(EXTRA_KEY_VIDEO_CONTENT, _content)
                .putExtra(EXTRA_KEY_VIDEO_ITAG, _vTag)
                .putExtra(EXTRA_KEY_AUDIO_ITAG, _aTag)
        }
    }

    companion object {
        const val EXTRA_KEY_URI_LIST = "com.amoscyk.android.rewatchplayer.service.URI_LIST"
        const val EXTRA_KEY_PLAYBACK_POSITION = "com.amoscyk.android.rewatchplayer.service.PLAYBACK_POSITION"
        const val EXTRA_KEY_VIDEO_ID = "com.amoscyk.android.rewatchplayer.service.VIDEO_ID"
        const val EXTRA_KEY_VIDEO_TITLE = "com.amoscyk.android.rewatchplayer.service.VIDEO_TITLE"
        const val EXTRA_KEY_VIDEO_CONTENT = "com.amoscyk.android.rewatchplayer.service.VIDEO_CONTENT"
        const val EXTRA_KEY_VIDEO_ITAG = "com.amoscyk.android.rewatchplayer.service.VIDEO_ITAG"
        const val EXTRA_KEY_AUDIO_ITAG = "com.amoscyk.android.rewatchplayer.service.AUDIO_ITAG"
        const val ACTION_GET_PLAYBACK_POSITION = "com.amoscyk.android.rewatchplayer.service.ACTION_GET_PLAYBACK_POSITION"
        const val PLAYBACK_CHANNEL_ID = "playbackChannelId"
        const val PLAYBACK_NOTIFICATION_ID = 1
    }

}
