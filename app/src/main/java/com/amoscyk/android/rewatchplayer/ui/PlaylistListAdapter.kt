package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist

class PlaylistListAdapter(
    private val itemOnClick: ((RPPlaylist) -> Unit)? = null
): ListAdapter<RPPlaylist, PlaylistListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.playlist_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val thumbnailImg = itemView.findViewById<ImageView>(R.id.playlist_img)
        private val titleTv = itemView.findViewById<TextView>(R.id.playlist_title_tv)

        fun bind(playlist: RPPlaylist) {
            thumbnailImg.load(playlist.thumbnails.default?.url)
            titleTv.text = playlist.title
            itemView.setOnClickListener {
                itemOnClick?.invoke(playlist)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RPPlaylist>() {
            override fun areItemsTheSame(oldItem: RPPlaylist, newItem: RPPlaylist): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: RPPlaylist, newItem: RPPlaylist): Boolean {
                return oldItem.id == newItem.id
                        && oldItem.title == newItem.title
            }
        }
    }
}