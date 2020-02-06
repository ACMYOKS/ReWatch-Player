package com.amoscyk.android.rewatchplayer.ui.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R

class PlayerSelectionAdapter<T: SelectableItemWithTitle, S: PlayerSelection<T>>(
    private var itemOnClick: ((option: S) -> Unit)? = null,
    diffUtil: DiffUtil.ItemCallback<S>
): ListAdapter<S, PlayerSelectionAdapter<T, S>.ViewHolder>(diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_selection_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
        holder.itemView.setOnClickListener {
            itemOnClick?.invoke(option)
        }
    }

    fun setItemOnClick(itemOnClick: (option: S) -> Unit) {
        this.itemOnClick = itemOnClick
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView as TextView

        fun bind(selection: S) {
            titleTv.text = selection.item.getTitle()
            titleTv.isSelected = selection.selected
        }
    }
}