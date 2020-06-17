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
import coil.transform.CircleCropTransformation
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.RPSubscription
import kotlinx.android.synthetic.main.subscription_list_item.view.*

class SubscriptionListAdapter(
    private val onItemClick: ((RPSubscription) -> Unit)? = null
): ListAdapter<RPSubscription, SubscriptionListAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var recyclerView: RecyclerView? = null
    private var isInfiniteLoadEnabled = true
    private var onLoadMoreNeeded: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.subscription_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
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
        private val thumbnail: ImageView = itemView.iv_thumbnail
        private val title: TextView = itemView.tv_title

        init {
            itemView.setOnClickListener { onItemClick?.invoke(getItem(adapterPosition)) }
        }

        fun bind(item: RPSubscription) {
            thumbnail.load(item.thumbnails.default?.url) {
                placeholder(R.drawable.ic_account_circle)
                error(R.drawable.ic_account_circle)
                transformations(CircleCropTransformation())
            }
            title.text = item.title
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RPSubscription>() {
            override fun areContentsTheSame(oldItem: RPSubscription, newItem: RPSubscription): Boolean {
                return oldItem.channelId == newItem.channelId
                        && oldItem.title == newItem.title
            }

            override fun areItemsTheSame(oldItem: RPSubscription, newItem: RPSubscription): Boolean {
                return oldItem == newItem
            }
        }
    }
}