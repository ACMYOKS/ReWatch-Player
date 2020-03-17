package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
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
    val mMotionLayout = LayoutInflater.from(context).inflate(R.layout.video_player_view, this, false) as MotionLayout
    private val mPlayerView: PlayerView
    private val mPlayerControlView: PlayerControlView
    private val mPlayerContainer: ConstraintLayout
    private val mAppBar: AppBarLayout
    private val mToolbar: Toolbar
    private val mTitleTv: TextView
    private val mCloseBtn: ImageButton
    private val mPlaybackBtn: ImageButton

    private val mTouchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var mOnDismissListener: OnPlayerDismissListener? = null

    private val mPlaybackListener: Player.EventListener

    init {
        addView(mMotionLayout)
        mPlayerView = mMotionLayout.findViewById(R.id.player_view)
        mPlayerControlView = mMotionLayout.findViewById(R.id.player_control_view)
        mPlayerContainer = mMotionLayout.findViewById(R.id.player_view_container)
        mAppBar = mMotionLayout.findViewById(R.id.app_bar_layout)
        mToolbar = mMotionLayout.findViewById(R.id.toolbar)
        mTitleTv = mMotionLayout.findViewById(R.id.tv_video_title)
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
                playWhenReady = !isPlaying
            }
        }
        mCloseBtn.setOnClickListener {
            mPlayerView.player.apply {
                stop()
            }
            mMotionLayout.transitionToState(R.id.video_player_dismiss)
        }
        mPlayerView.useController = false          // use custom handling controller

        mMotionLayout.setTransitionListener(object : TransitionListener {
            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {

            }

            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                mOnDismissListener?.onDismissStarted()
                mPlayerControlView.hide()
            }

            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {

            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                mOnDismissListener?.onDismissCompleted()
                if (currentId == R.id.video_player_fullscreen) {
                    mPlayerControlView.show()
                }
            }

        })

        mPlayerControlView.setVisibilityListener { visibility ->
            mAppBar.visibility = visibility
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val isInProgress = (mMotionLayout.progress > 0.0f && mMotionLayout.progress < 1.0f)
        val isInTarget = isTouchEventInsideTarget(mPlayerContainer, event)
        Log.d("MOMO", "intercept: isInProgress $isInProgress isInTarget $isInTarget")
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
            Log.d("MOMO", "touch inside click")
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x
                    startY = ev.y
                    Log.d("MOMO", "ACTION DOWN: $startX $startY")
                }

                MotionEvent.ACTION_UP -> {
                    val endX = ev.x
                    val endY = ev.y
                    if (isClick(startX, startY, endX, endY)) {
                        toggleControlView()
                        return true
                    } else {

                        Log.d("MOMO", "ACTION UP, is not click")
                    }
                }
            }
        }
        else {
            Log.d("AMOS", "not touch inside click")
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }

    private fun toggleControlView() {
        if (mMotionLayout.currentState != R.id.video_player_fullscreen) return
        if (mPlayerControlView.isVisible) {
            mPlayerControlView.hide()
        } else {
            mPlayerControlView.show()
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
    }

    fun setOnDismissListener(listener: OnPlayerDismissListener) {
        mOnDismissListener = listener
    }

    fun getToolbar() = mToolbar

    enum class PlayerSize { FULLSCREEN, SMALL, DISMISS }

    interface OnPlayerDismissListener {
        fun onDismissStarted()
        fun onDismissCompleted()
    }

}