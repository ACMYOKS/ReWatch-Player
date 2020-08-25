package com.amoscyk.android.rewatchplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.motion.widget.MotionLayout
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.isTouchEventInsideTarget
import java.lang.ref.WeakReference
import kotlin.math.abs

class InterveneTransitionMotionLayout : MotionLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isTransitionEnabled = true
    private var startX = 0f
    private var startY = 0f

    private val onClickReceiverList = ArrayList<OnClickReceiver>()
    private val ignoreTransitionViewList = ArrayList<WeakReference<View>>()

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onClickReceiverList.forEach { receiver ->
            if (receiver.view.isTouchEventInsideTarget(ev)) {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = ev.x
                        startY = ev.y
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick(startX, startY, ev.x, ev.y)) {
                            receiver.onClick(receiver.view)
                        }
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        ignoreTransitionViewList.forEach { ref ->
            ref.get()?.let { v ->
                if (v.isTouchEventInsideTarget(event)) {
                    return false
                }
            }
        }
        if (!isTransitionEnabled) return false
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return super.onTouchEvent(event)
    }

    fun setStateAndDisableOtherTransitions(stateId: Int) {
        if (currentState != stateId) {
            setTransition(stateId, stateId)
            setTransitionDuration(0)
        }
    }

    private fun isClick(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        return abs(startX - endX) < touchSlop && abs(startY - endY) < touchSlop
    }

    fun registerViewForOnClickEvent(view: View, onClick: (View) -> Unit) {
        onClickReceiverList.add(OnClickReceiver(view, onClick))
    }

    fun addIgnoreTransitionView(view: View) {
        ignoreTransitionViewList.add(WeakReference(view))
    }

    data class OnClickReceiver(val view: View, val onClick: (View) -> Unit)

}