package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.util.DateTimeHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper

class VideoListAdapter: ListAdapter<VideoMeta, VideoListAdapter.ViewHolder>(DIFF_CALLBACK) {

    var isEditMode = false
        private set
    var isArchivable = false
        private set
    var isBookmarkable = false
        private set
    private var shouldShowLoading = false

    private var onItemClick: ((position: Int, meta: VideoMeta) -> Unit)? = null
    private var onItemLongClick: ((position: Int, meta: VideoMeta) -> Boolean)? = null
    private var onArchiveClick: ((position: Int, meta: VideoMeta) -> Unit)? = null
    private var onBookmarkClick: ((position: Int, meta: VideoMeta) -> Unit)? = null

    private val viewStatusMap = hashMapOf<String, ViewStatus>()

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

    fun setArchivable(value: Boolean) {
        if (isArchivable != value) {
            isArchivable = value
            notifyDataSetChanged()
        }
    }

    fun setBookmarkable(value: Boolean) {
        if (isBookmarkable != value) {
            isBookmarkable = value
            notifyDataSetChanged()
        }
    }

    fun setEditMode(value: Boolean) {
        if (isEditMode != value) {
            isEditMode = value
            if (!isEditMode) {
                viewStatusMap.keys.forEach {
                    viewStatusMap[it]!!.isSelected = false
                }
            }
            notifyDataSetChanged()
        }
    }

    fun toggleItemSelection(position: Int) {
        if (position in 0 until itemCount) {
            viewStatusMap[getItem(position).videoId]?.apply {
                isSelected = !isSelected
                notifyItemChanged(position)
            }
        }
    }

    fun setOnItemClickListener(l: ((position: Int, meta: VideoMeta) -> Unit)?) { onItemClick = l }
    fun setOnItemLongClickListener(l: ((position: Int, meta: VideoMeta) -> Boolean)?) { onItemLongClick = l }
    fun setOnArchiveClickListener(l: ((position: Int, meta: VideoMeta) -> Unit)?) { onArchiveClick = l }
    fun setOnBookmarkClickListener(l: ((position: Int, meta: VideoMeta) -> Unit)?) { onBookmarkClick = l }

    private data class ViewStatus(var isSelected: Boolean = false)

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val thumbnailIv: ImageView = itemView.findViewById(R.id.thumbnail)
        val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        val channelTitleTv: TextView = itemView.findViewById(R.id.channel_title_tv)
        val durationTv: TextView = itemView.findViewById(R.id.duration_tv)
        val archiveBtn: ImageView = itemView.findViewById(R.id.iv_archive)
        val bookmarkBtn: ImageView = itemView.findViewById(R.id.iv_bookmark)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)
        val progressBar: ContentLoadingProgressBar = itemView.findViewById(R.id.progress_bar)

        init {
            cardView.apply {
                setOnClickListener { onItemClick?.invoke(adapterPosition, getItem(adapterPosition)) }
                setOnLongClickListener { onItemLongClick?.invoke(adapterPosition, getItem(adapterPosition)) ?: true }
            }
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                viewStatusMap[getItem(adapterPosition).videoId]?.isSelected = isChecked
            }
            archiveBtn.setOnClickListener { onArchiveClick?.invoke(adapterPosition, getItem(adapterPosition)) }
            bookmarkBtn.setOnClickListener { onBookmarkClick?.invoke(adapterPosition, getItem(adapterPosition)) }
        }

        fun bind(video: VideoMeta, position: Int) {
            if (viewStatusMap[video.videoId] == null) {
                viewStatusMap[video.videoId] = ViewStatus()
            }
            titleTv.text = video.title
            channelTitleTv.text = video.channelTitle
            durationTv.text = DateTimeHelper.getDisplayString(video.duration)
            thumbnailIv.load(
                video.thumbnails.standard?.url
                    ?: YouTubeVideoThumbnailHelper.getStandardUrl(video.videoId)
            ) {
                placeholder(R.drawable.ic_broken_image_white)
                error(R.drawable.ic_broken_image_white)
            }
            if (position == itemCount - 1 && shouldShowLoading) progressBar.show()
            else progressBar.hide()
            checkbox.visibility = if (isEditMode) View.VISIBLE else View.GONE
            checkbox.isChecked = viewStatusMap[video.videoId]!!.isSelected
            archiveBtn.visibility = if (isArchivable) View.VISIBLE else View.GONE
            bookmarkBtn.visibility = if (isBookmarkable) View.VISIBLE else View.GONE
            bookmarkBtn.isSelected = video.bookmarked
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