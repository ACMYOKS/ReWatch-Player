package com.amoscyk.android.rewatchplayer.ui.player

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.getColorFromAttr

class PlayerOptionAdapter(
    private val itemOnClick: ((option: PlayerOption, position: Int) -> Unit)? = null,
    private val itemOnCheckedChange: ((isChecked: Boolean, position: Int) -> Unit)? = null
): ListAdapter<PlayerOption, PlayerOptionAdapter.ViewHolder>(DIFF_UTIL) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.player_option_cell, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.image)
        private val titleTv: TextView = itemView.findViewById(R.id.title_tv)
        private val switchCheck: Switch = itemView.findViewById(R.id.switch_check)

        init {
            itemView.setOnClickListener {
                itemOnClick?.invoke(getItem(adapterPosition), adapterPosition)
            }
            switchCheck.setOnClickListener {
                val isChecked = switchCheck.isChecked
                getItem(adapterPosition).checked = isChecked
                itemOnCheckedChange?.invoke(isChecked, adapterPosition)
            }
        }

        fun bind(option: PlayerOption) {
            imageView.apply {
                setImageResource(option.imgResId)
                setColorFilter(context.getColorFromAttr(R.attr.colorOnSurface))
            }
            titleTv.text = option.title
            switchCheck.apply {
                visibility = if (option.checked == null) View.GONE else View.VISIBLE
                isChecked = option.checked ?: false
            }
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