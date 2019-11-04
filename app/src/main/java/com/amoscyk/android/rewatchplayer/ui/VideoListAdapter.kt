package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo

class VideoListAdapter: ListAdapter<RPVideo, VideoListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val videoItemView = inflater.inflate(R.layout.video_list_item, parent, false)
        return ViewHolder(videoItemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val cardView = itemView.findViewById<CardView>(R.id.card_view)
        private val thumbnailIV = itemView.findViewById<ImageView>(R.id.thumbnail)
        private val titleTv = itemView.findViewById<TextView>(R.id.title_tv)
        private val channelTitleTv = itemView.findViewById<TextView>(R.id.channel_title_tv)

        fun bind(video: RPVideo) {
            titleTv.text = video.title
            channelTitleTv.text = video.channelTitle
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RPVideo>() {
            override fun areItemsTheSame(oldItem: RPVideo, newItem: RPVideo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: RPVideo, newItem: RPVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
}