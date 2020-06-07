package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class VideoPlayerLayout
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MotionLayout(context, attrs, defStyleAttr) {
    private val mMotionLayout = LayoutInflater.from(context).inflate(R.layout.video_player_view, this, false) as MotionLayout
    private val mPlayerView: PlayerView
    private val mPlayerControlView: PlayerControlView
    private val mPlayerContainer: ConstraintLayout
    private val mAppBar: AppBarLayout
    private val mToolbar: Toolbar
    private val mTitleTv: TextView
    private val mSmallTitleTv: TextView
    private val mCloseBtn: ImageButton
    private val mPlaybackBtn: ImageButton

    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var mSizeListener: PlayerSizeListener? = null
    private var mEnableSlide = true

    private val mPlaybackListener: Player.EventListener

    init {
        addView(mMotionLayout)
        mPlayerView = mMotionLayout.findViewById(R.id.player_view)
        mPlayerControlView = mMotionLayout.findViewById(R.id.player_control_view)
        mPlayerContainer = mMotionLayout.findViewById(R.id.player_view_container)
        mAppBar = mMotionLayout.findViewById(R.id.app_bar_layout)
        mToolbar = mMotionLayout.findViewById(R.id.toolbar)
        mTitleTv = mMotionLayout.findViewById(R.id.tv_video_title)
        mSmallTitleTv = mMotionLayout.findViewById(R.id.tv_video_title_small)
        mCloseBtn = mMotionLayout.findViewById(R.id.btn_close_small)
        mPlaybackBtn = mMotionLayout.findViewById(R.id.btn_playback_small)
        mPlaybackListener = object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                mPlaybackBtn.setImageResource(
                    if (isPlaying) R.drawable.ic_pause_white
                    else R.drawable.ic_play_arrow_white
                )
            }
        }
        mPlaybackBtn.setOnClickListener {
            mPlayerView.player.apply {
                // toggle play/pause
                playWhenReady = !isPlaying
            }
        }
        mCloseBtn.setOnClickListener {
            mMotionLayout.transitionToState(R.id.video_player_dismiss)
        }
        mPlayerView.apply {
            useController = false       // use custom handling controller
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        mTitleTv.isSelected = true              // enable marquee
        mSmallTitleTv.isSelected = true              // enable marquee

        mMotionLayout.setTransitionListener(object : TransitionListener {
            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

            }

            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                mPlayerControlView.hide()
                mSizeListener?.onStart(PlayerSize.fromRes(startId), PlayerSize.fromRes(endId))
            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.video_player_fullscreen) {
                    Log.d(AppConstant.TAG, "$TAG: transition completed, is fullscreen, show control view")
                    // sync visibility setting of player control view with motion layout
                    Handler().postDelayed({ mPlayerControlView.show() }, 0)
                } else if (currentId == R.id.video_player_dismiss) {
                    Log.d(AppConstant.TAG, "TAG: transition completed, is dismiss, stop video")
                    mPlayerView.player.stop()
                }
                mSizeListener?.onComplete(PlayerSize.fromRes(currentId))
            }

        })

        mPlayerControlView.setVisibilityListener { visibility ->
            mAppBar.visibility = visibility
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val isInProgress = (mMotionLayout.progress > 0.0f && mMotionLayout.progress < 1.0f)
        val isInTarget = isTouchEventInsideTarget(mPlayerContainer, event)
//        Log.d(AppConstant.TAG, "$TAG: intercept: isInProgress $isInProgress isInTarget $isInTarget")
        return if (isInProgress || isInTarget) {
            super.onInterceptTouchEvent(event)
        } else {
            true
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isTouchEventInsideTarget(mPlayerControlView, ev) || isTouchEventInsideTarget(mAppBar, ev)) {
            return super.dispatchTouchEvent(ev)
        }
        if (isTouchEventInsideTarget(mPlayerView, ev)) {
//            Log.d(AppConstant.TAG, "$TAG: touch inside click")
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x
                    startY = ev.y
//                    Log.d(AppConstant.TAG, "$TAG: ACTION DOWN: $startX $startY")
                    return if (mEnableSlide) super.dispatchTouchEvent(ev) else true
                }

                MotionEvent.ACTION_UP -> {
                    val endX = ev.x
                    val endY = ev.y
                    if (isClick(startX, startY, endX, endY)) {
                        transformToFullscreenIfNeeded()
                        toggleControlView()
                        return true
                    } else {
//                        Log.d(AppConstant.TAG, "$TAG: ACTION UP, is not click")
                    }
                }
            }
        }
//        else {
//            Log.d(AppConstant.TAG, "$TAG: not touch inside click")
//        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    private fun toggleControlView() {
        if (mMotionLayout.currentState == R.id.video_player_fullscreen) {
            if (mPlayerControlView.isVisible) {
                mPlayerControlView.hide()
            } else {
                mPlayerControlView.show()
            }
        }
    }

    private fun transformToFullscreenIfNeeded() {
        if (mMotionLayout.currentState == R.id.video_player_small) {
            mMotionLayout.transitionToState(R.id.video_player_fullscreen)
        }
    }

    private fun isTouchEventInsideTarget(v: View, ev: MotionEvent): Boolean {
        return ev.x in v.left.toFloat()..v.right.toFloat() && ev.y in v.top.toFloat()..v.bottom.toFloat()
    }

    private fun isClick(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        return abs(startX-endX) < mTouchSlop && abs(startY-endY) < mTouchSlop
    }

    fun setPlayer(exoPlayer: ExoPlayer?) {
        mPlayerView.player = exoPlayer
        mPlayerControlView.player = exoPlayer
        exoPlayer?.addListener(mPlaybackListener)
    }

    fun setPlayerSize(playerSize: PlayerSize) {
        when (playerSize) {
            PlayerSize.FULLSCREEN -> {
                mMotionLayout.transitionToState(R.id.video_player_fullscreen)
            }
            PlayerSize.SMALL -> {
                mMotionLayout.transitionToState(R.id.video_player_small)
            }
            PlayerSize.DISMISS -> {
                mMotionLayout.transitionToState(R.id.video_player_dismiss)
            }
        }
    }

    fun setTitle(title: String) {
        mTitleTv.text = title
        mSmallTitleTv.text = title
    }

    fun setPlayerSizeListener(listener: PlayerSizeListener) {
        mSizeListener = listener
    }

    fun setEnableTransition(enable: Boolean) { mEnableSlide = enable }

    fun isTransitionEnabled(): Boolean = mEnableSlide

    val toolbar get() = mToolbar

    val isFullscreen get() = mMotionLayout.currentState == R.id.video_player_fullscreen
    val isSmall get() = mMotionLayout.currentState == R.id.video_player_small
    val isDismiss get() = mMotionLayout.currentState == R.id.video_player_dismiss

    enum class PlayerSize {
        FULLSCREEN, SMALL, DISMISS;
        companion object {
            fun fromRes(resId: Int): PlayerSize = when (resId) {
                R.id.video_player_fullscreen -> FULLSCREEN
                R.id.video_player_small -> SMALL
                else -> DISMISS
            }
        }
    }

    interface PlayerSizeListener {
        fun onStart(start: PlayerSize, end: PlayerSize)
        fun onComplete(current: PlayerSize)
    }

    companion object {
        private const val TAG = "VideoPlayerLayout"
    }

}