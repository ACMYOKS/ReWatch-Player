package com.amoscyk.android.rewatchplayer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist

class PlaylistListAdapter(
    private val itemOnClick: ((RPPlaylist) -> Unit)? = null
): ListAdapter<RPPlaylist, PlaylistListAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var recyclerView: RecyclerView? = null
    private var isInfiniteLoadEnabled = true
    private var onLoadMoreNeeded: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.playlist_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isInfiniteLoadEnabled) {
                    if (dy > 0) {
                        (recyclerView.layoutManager as? LinearLayoutManager)?.apply {
                            if (findLastCompletelyVisibleItemPosition() == itemCount - 1) {
                                onLoadMoreNeeded?.invoke()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun setEnableInfiniteLoad(value: Boolean) {
        isInfiniteLoadEnabled = value
    }

    fun setOnLoadMoreNeeded(l: (() -> Unit)?) {
        onLoadMoreNeeded = l
    }


    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val thumbnailImg = itemView.findViewById<ImageView>(R.id.playlist_img)
        private val titleTv = itemView.findViewById<TextView>(R.id.playlist_title_tv)

        init {
            itemView.setOnClickListener {
                itemOnClick?.invoke(getItem(adapterPosition))
            }
        }

        fun bind(playlist: RPPlaylist) {
            thumbnailImg.load(playlist.thumbnails.run { standard?.url ?: default?.url })
            titleTv.text = playlist.title
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