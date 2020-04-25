package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.util.YouTubeThumbnailHelper

class VideoListAdapter(
    private val onItemClick: ((VideoMeta) -> Unit)? = null
): ListAdapter<VideoMeta, VideoListAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var shouldShowLoading = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val videoItemView = inflater.inflate(R.layout.video_list_item, parent, false)
        return ViewHolder(videoItemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video, position)
    }

    // show/hide progress bar when there is item to load or not
    fun setShowLoadingAtBottom(show: Boolean) {
        if (shouldShowLoading != show) {
            shouldShowLoading = show
            notifyItemChanged(itemCount - 1)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val thumbnailIv: ImageView = itemView.findViewById(R.id.thumbnail)
        val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        val channelTitleTv: TextView = itemView.findViewById(R.id.channel_title_tv)
        val progressBar: ContentLoadingProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            cardView.apply {
                setOnClickListener { onItemClick?.invoke(getItem(adapterPosition)) }
            }
        }

        fun bind(video: VideoMeta, position: Int) {
            titleTv.text = video.title
            channelTitleTv.text = video.channelTitle
            thumbnailIv.load(
                video.thumbnails.standard?.url
                    ?: YouTubeThumbnailHelper.getStandardUrl(video.videoId)
            ) {
                placeholder(R.drawable.ic_broken_image_white)
                error(R.drawable.ic_broken_image_white)
            }
            if (position == itemCount - 1 && shouldShowLoading) progressBar.show()
            else progressBar.hide()
        }

    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VideoMeta>() {
            override fun areItemsTheSame(oldItem: VideoMeta, newItem: VideoMeta): Boolean {
                return oldItem.videoId == newItem.videoId &&
                        oldItem.bookmarked == newItem.bookmarked
            }

            override fun areContentsTheSame(oldItem: VideoMeta, newItem: VideoMeta): Boolean {
                return oldItem == newItem
            }
        }
    }
}