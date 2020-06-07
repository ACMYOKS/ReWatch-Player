package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class VideoInfoBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    private var mContentView: View? = null
    private var mTvVideoTitle: TextView? = null
    private var mTvChannelTitle: TextView? = null
    private var mTvVideoId: TextView? = null
    private var mTvDescription: TextView? = null
    private var mTvTags: TextView? = null
    private var mChipGroupTags: ChipGroup? = null
    private var videoMeta: VideoMeta? = null

    private var mViewChannelOnClickListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mContentView = LayoutInflater.from(context).inflate(R.layout.dialog_video_info, null, false)
        setContentView(mContentView!!)
        mTvVideoTitle = mContentView!!.findViewById(R.id.tv_video_title)
        mTvChannelTitle = mContentView!!.findViewById(R.id.tv_channel_title)
        mTvVideoId = mContentView!!.findViewById(R.id.tv_video_id)
        mTvDescription = mContentView!!.findViewById(R.id.tv_video_description)
        mTvTags = mContentView!!.findViewById(R.id.tv_video_tags)
        mChipGroupTags = mContentView!!.findViewById(R.id.chip_group_video_tags)
        mTvChannelTitle!!.setOnClickListener { mViewChannelOnClickListener?.onClick(it) }
        super.onCreate(savedInstanceState)
        refreshView()
    }

    override fun show() {
        super.show()
        BottomSheetBehavior.from(mContentView!!.parent as View).state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setVideoMeta(videoMeta: VideoMeta) {
        this.videoMeta = videoMeta
        refreshView()
    }

    fun setViewChannelButtonOnClickListener(listener: View.OnClickListener) {
        mViewChannelOnClickListener = listener
    }

    private fun refreshView() {
        videoMeta?.let { meta ->
            mTvVideoTitle?.text = meta.title
            mTvChannelTitle?.text = meta.channelTitle.let {
                SpannableString(it).apply {
                    setSpan(UnderlineSpan(), 0, it.length, 0)
                }
            }
            mTvVideoId?.text = meta.videoId
            mTvDescription?.text = meta.description
            mChipGroupTags?.apply {
                removeAllViews()
                meta.tags.forEach {
                    val chip =
                        layoutInflater.inflate(R.layout.info_tag_action_chip, this, false) as Chip
                    chip.text = it
                    chip.isCloseIconVisible = false
                    chip.isCheckable = false
                    chip.setOnClickListener {  }
                    addView(chip)
                }
            }
        }
    }

}