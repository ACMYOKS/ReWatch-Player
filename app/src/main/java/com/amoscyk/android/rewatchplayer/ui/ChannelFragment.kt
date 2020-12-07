package com.amoscyk.android.rewatchplayer.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import coil.Coil
import coil.api.load
import coil.request.LoadRequest
import coil.transform.BlurTransformation
import coil.transform.CircleCropTransformation
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.RPPlaylist
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.util.dpToPx
import com.amoscyk.android.rewatchplayer.util.setForegroundItemColor
import com.amoscyk.android.rewatchplayer.util.withEmoji
import com.amoscyk.android.rewatchplayer.viewModelFactory
import kotlinx.android.synthetic.main.fragment_channel.view.*

class ChannelFragment: ReWatchPlayerFragment() {

    private var mRootView: View? = null
    private val mCollapsingToolbar by lazy { mRootView!!.collapsing_toolbar }
    private val mToolbar by lazy { mRootView!!.toolbar }
    private val mSvContent by lazy { mRootView!!.sv_content }
    private val mIvThumbnail by lazy { mRootView!!.iv_thumbnail }
    private val mTvChannelName by lazy { mRootView!!.tv_title }
    private val mIvThumbnailToolbar by lazy { mRootView!!.iv_thumbnail_toolbar }
    private val mTvChannelNameToolbar by lazy { mRootView!!.tv_title_toolbar }
    private val mTvSubscriberCount by lazy { mRootView!!.tv_subscriber_count }
    private val mTvVideoCount by lazy { mRootView!!.tv_video_count }
    private val mTvDescription by lazy { mRootView!!.tv_description }
    private val mBtnToggleDetail by lazy { mRootView!!.btn_toggle_description }
    private val mRvUploaded by lazy { mRootView!!.rv_uploaded }
    private val mRvPlaylist by lazy { mRootView!!.rv_playlist }
    private val mLoadingUploaded by lazy { mRootView!!.loading_upload }
    private val mLoadingPlaylist by lazy { mRootView!!.loading_playlist }
    private val mEmptyViewUploaded by lazy { mRootView!!.empty_view1 }
    private val mEmptyViewPlaylist by lazy { mRootView!!.empty_view2 }

    private val mUploadedAdapter = PlaylistAdapter(itemOnClick = {
        findNavController().navigate(VideoListFragmentDirections.showVideoListForPlaylist(it, true))
    }).apply { setEnableInfiniteLoad(false) }
    private val mPlaylistAdapter = PlaylistAdapter(itemOnClick = {
        findNavController().navigate(VideoListFragmentDirections.showVideoListForPlaylist(it, true))
    }, onReloadNeeded = {
        viewModel.loadMorePlaylist()
    })

    private val viewModel by viewModels<ChannelViewModel> { viewModelFactory }
    private val args by navArgs<ChannelFragmentArgs>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        handleGoogleUserAuthEvent(viewModel)

        viewModel.showLoadingChannel.observe(this, Observer { event ->
            event.getContentIfNotHandled {

            }
        })

        viewModel.channelList.observe(this, Observer { res ->
            res.items.firstOrNull()?.let { info ->
                val title = info.title.withEmoji()
                mTvChannelName.text = title
                mTvChannelNameToolbar.text = title
                mTvDescription.text = info.description.withEmoji()
                mTvSubscriberCount.text = getString(R.string.channel_subscriber_count).format(info.subscriberCount)
                mTvVideoCount.text = getString(R.string.channel_video_count).format(info.videoCount)
                mIvThumbnail.load(info.thumbnails.run { standard?.url ?: default?.url }) {
                    transformations(CircleCropTransformation())
                }
                mIvThumbnailToolbar.load(info.thumbnails.run { standard?.url ?: default?.url }) {
                    transformations(CircleCropTransformation())
                }
                viewModel.setUploadedListId(info.uploads)
            }
        })

        viewModel.showLoadingUploaded.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (it.isLoading) mLoadingUploaded.show() else mLoadingUploaded.hide()
            }
        })

        viewModel.uploadedList.observe(this, Observer {
            mUploadedAdapter.insertList(it.items)
            mEmptyViewUploaded.visibility =
                if (mUploadedAdapter.itemCount == 0) View.VISIBLE
                else View.GONE
        })

        viewModel.showLoadingPlaylist.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) mLoadingPlaylist.show() else mLoadingPlaylist.hide()
                }
            }
        })

        viewModel.featuredList.observe(this, Observer {
            mPlaylistAdapter.onUpdateFinish()
            mPlaylistAdapter.insertList(it.newItems)
            mPlaylistAdapter.setEnableInfiniteLoad(!it.isEndOfList)
            mEmptyViewPlaylist.visibility =
                if (mPlaylistAdapter.itemCount == 0) View.VISIBLE
                else View.GONE
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_channel, container, false)
            setupViews()
        }
        return mRootView
    }

    override fun onStart() {
        super.onStart()
        viewModel.setChannelId(args.channelId)
    }

    private fun setupViews() {
        mCollapsingToolbar.apply {
            setupWithNavController(mToolbar, findNavController())
            setExpandedTitleColor(Color.TRANSPARENT)
        }
        mBtnToggleDetail.setOnClickListener {
            it.isSelected = !it.isSelected
            TransitionManager.beginDelayedTransition(mRootView as ViewGroup,
                Fade().apply { addTarget(mTvDescription) })
            mTvDescription.visibility = if (it.isSelected) View.VISIBLE else View.GONE
        }
        mBtnToggleDetail.isSelected = false
        mTvDescription.visibility = View.GONE
        mRvUploaded.apply {
            adapter = mUploadedAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(dpToPx(8f).toInt(), dpToPx(14f).toInt()))
        }
        mRvPlaylist.apply {
            adapter = mPlaylistAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(dpToPx(8f).toInt(), dpToPx(14f).toInt()))
        }
        mSvContent.setOnScrollChangeListener { v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            val channelY = mTvChannelName.let { it.y + it.height }
            if (scrollY > channelY) {
                mTvChannelNameToolbar.changeAlpha(1f)
                mIvThumbnailToolbar.changeAlpha(1f)
            } else {
                mTvChannelNameToolbar.changeAlpha(0f)
                mIvThumbnailToolbar.changeAlpha(0f)
            }
        }
        mTvChannelNameToolbar.alpha = 0f
        mIvThumbnailToolbar.alpha = 0f
    }

    private fun View.changeAlpha(newAlpha: Float) {
        animate().apply {
            interpolator = LinearInterpolator()
            alpha(newAlpha)
            duration = 300
        }.start()
    }

    private class PlaylistAdapter(
        private val itemOnClick: ((RPPlaylist) -> Unit)? = null,
        private val onReloadNeeded: (() -> Unit)? = null
    ) : RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {
        private val list = arrayListOf<RPPlaylist>()
        private val reloadThresholdMilli = 5000L     // after trigger reload, disable trigger for this period of time
        private var disableReloadUntil = -1L
        private var enableInfiniteLoad = true

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.playlist_list_item, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.bind(item)
            if (enableInfiniteLoad && position == itemCount - 1) {
                val current = System.currentTimeMillis()
                if (current >= disableReloadUntil) {
                    onReloadNeeded?.invoke()
                    disableReloadUntil = current + reloadThresholdMilli
                }
            }
        }

        fun setEnableInfiniteLoad(value: Boolean) {
            enableInfiniteLoad = value
        }

        fun onUpdateFinish() {
            disableReloadUntil = -1L
        }

        fun insertList(newList: List<RPPlaylist>) {
            list.apply { addAll(newList) }
            notifyDataSetChanged()
        }

        fun setList(newList: List<RPPlaylist>) {
            list.apply { clear(); addAll(newList) }
            notifyDataSetChanged()
        }

        private inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
            val thumbnailImg = itemView.findViewById<ImageView>(R.id.playlist_img)
            val titleTv = itemView.findViewById<TextView>(R.id.playlist_title_tv)
            init {
                itemView.setOnClickListener {
                    itemOnClick?.invoke(list[adapterPosition])
                }
            }

            fun bind(playlist: RPPlaylist) {
                thumbnailImg.load(playlist.thumbnails.default?.url)
                titleTv.text = playlist.title
            }
        }
    }
}