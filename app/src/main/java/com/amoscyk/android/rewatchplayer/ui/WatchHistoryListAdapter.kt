package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.local.WatchHistoryVideoMeta
import com.amoscyk.android.rewatchplayer.util.DateTimeHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper

class WatchHistoryListAdapter : ListAdapter<WatchHistoryVideoMeta, WatchHistoryListAdapter.ViewHolder>(DIFF_CALLBACK) {
    var isEditMode = false
        private set
    private var onItemClick: ((position: Int, meta: WatchHistoryVideoMeta) -> Unit)? = null
    private var onItemLongClick: ((position: Int, meta: WatchHistoryVideoMeta) -> Boolean)? = null
    private val viewStatusMap = hashMapOf<String, ViewStatus>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.watch_history_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).videoId.hashCode().toLong()
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

    fun getSelectedItemsId(): List<String> =
        viewStatusMap.filterValues { it.isSelected }.keys.toList()

    fun setOnItemClickListener(l: ((position: Int, meta: WatchHistoryVideoMeta) -> Unit)?) { onItemClick = l }
    fun setOnItemLongClickListener(l: ((position: Int, meta: WatchHistoryVideoMeta) -> Boolean)?) { onItemLongClick = l }

    private data class ViewStatus(var isSelected: Boolean = false)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val thumbnailIv: ImageView = itemView.findViewById(R.id.thumbnail)
        val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        val channelTitleTv: TextView = itemView.findViewById(R.id.channel_title_tv)
        val durationTv: TextView = itemView.findViewById(R.id.duration_tv)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_select)
        val progressBar: ProgressBar = itemView.findViewById(R.id.watch_progress)

        init {
            cardView.apply {
                setOnClickListener { onItemClick?.invoke(adapterPosition, getItem(adapterPosition)) }
                setOnLongClickListener { onItemLongClick?.invoke(adapterPosition, getItem(adapterPosition)) ?: true }
            }
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                viewStatusMap[getItem(adapterPosition).videoId]?.isSelected = isChecked
            }
        }

        fun bind(history: WatchHistoryVideoMeta, position: Int) {
            if (viewStatusMap[history.videoId] == null) {
                viewStatusMap[history.videoId] = ViewStatus()
            }
            titleTv.text = history.title
            channelTitleTv.text = history.channelTitle
            durationTv.text = DateTimeHelper.getDisplayString(history.duration)
            thumbnailIv.load(YouTubeVideoThumbnailHelper.getStandardUrl(history.videoId)) {
                placeholder(R.drawable.ic_image)
                error(R.drawable.ic_image)
            }
            progressBar.progress =
                (history.lastWatchPosMillis.toFloat() / DateTimeHelper.getDurationMillis(history.duration) * 100).toInt()
            checkbox.visibility = if (isEditMode) View.VISIBLE else View.GONE
            checkbox.isChecked = viewStatusMap[history.videoId]!!.isSelected
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WatchHistoryVideoMeta>() {
            override fun areItemsTheSame(
                oldItem: WatchHistoryVideoMeta,
                newItem: WatchHistoryVideoMeta
            ): Boolean {
                return oldItem.videoId == newItem.videoId
                        && oldItem.lastWatchPosMillis == newItem.lastWatchPosMillis
                        && oldItem.recentWatchDateTimeMillis == newItem.recentWatchDateTimeMillis
            }

            override fun areContentsTheSame(
                oldItem: WatchHistoryVideoMeta,
                newItem: WatchHistoryVideoMeta
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}