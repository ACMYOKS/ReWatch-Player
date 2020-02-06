package com.amoscyk.android.rewatchplayer.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R

class PlayerOptionAdapter(
    private val itemOnClick: ((option: PlayerOption, position: Int) -> Unit)? = null
): ListAdapter<PlayerOption, PlayerOptionAdapter.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_option_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
        holder.itemView.setOnClickListener {
            itemOnClick?.invoke(option, position)
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.image)
        private val titleTv: TextView = itemView.findViewById(R.id.title_tv)

        fun bind(option: PlayerOption) {
            imageView.setImageResource(option.imgResId)
            titleTv.text = option.title
        }
    }

    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<PlayerOption>() {
            override fun areItemsTheSame(oldItem: PlayerOption, newItem: PlayerOption): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: PlayerOption, newItem: PlayerOption): Boolean {
                return oldItem.title == newItem.title
            }
        }
    }
}