package com.amoscyk.android.rewatchplayer.ui


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.util.dpToPx
import com.amoscyk.android.rewatchplayer.viewModelFactory
import kotlinx.coroutines.launch

class VideoListFragment : ReWatchPlayerFragment() {

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<VideoListViewModel> { viewModelFactory }

    private var videoMetas = listOf<VideoMeta>()

    private val mOnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.setEditMode(false)
        }
    }

    private val args by navArgs<VideoListFragmentArgs>()
    private var rootView: View? = null
    private lateinit var mToolbar: Toolbar
    private lateinit var mVideoListView: RecyclerView
    private lateinit var mLoadingView: ContentLoadingProgressBar
    private lateinit var mButton: Button
    private val mListAdapter = VideoListAdapter().apply {
        setArchivable(true)
        setBookmarkable(true)
        setOnArchiveClickListener { position, meta ->
            mainActivity?.showArchiveOption(meta.videoId)
        }
        setOnBookmarkClickListener { position, meta ->
            mainViewModel.toggleBookmarkStatus(meta.videoId)
        }
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
            } else {
                mainActivity?.playVideoForId(meta.videoId)
            }
        }
        setOnItemLongClickListener { position, meta ->
            if (args.enableEditOnLongClick) {
                viewModel.setEditMode(true)
                toggleItemSelection(position)
            }
            true
        }
        setOnLoadMoreNeeded {
            viewModel.loadMoreVideos()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(mOnBackPressedCallback)

        mainViewModel.bookmarkToggled.observe(this, Observer { videoId ->
            val idx = videoMetas.indexOfFirst { it.videoId == videoId }
            if (idx != -1) {
                videoMetas[idx].bookmarked = !videoMetas[idx].bookmarked
                mListAdapter.notifyItemChanged(idx)
            }
        })

        viewModel.title.observe(this, Observer { title ->
            mToolbar.title = title
        })

        viewModel.showListItemLoading.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (mListAdapter.itemCount == 0) {
                    if (it) mLoadingView.show() else mLoadingView.hide()
                } else {
                    mListAdapter.setShowLoadingAtBottom(it)
                }
            }
        })

        viewModel.showVideoLoading.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (mListAdapter.itemCount == 0) {
                    if (it) mLoadingView.show() else mLoadingView.hide()
                } else {
                    mListAdapter.setShowLoadingAtBottom(it)
                }
            }
        })

        viewModel.showVideoLoading.observe(this, Observer {

        })

        viewModel.playlistItemList.observe(this, Observer {
            viewModel.setPlaylistItems(it.newItems)
            mListAdapter.setEnableInfiniteLoad(!it.isEndOfList)
        })

        viewModel.videoList.observe(this, Observer {
            lifecycleScope.launch {
                videoMetas = videoMetas + mainViewModel.getVideoMetas(it.newItems)
                mListAdapter.submitList(videoMetas)
            }
        })

//        viewModel.playlistResource.observe(this, Observer { res ->
//            when (res.status) {
//                Status.SUCCESS -> {
//                    mLoadingView.hide()
//                    viewModel.setPlaylistItems(res.data?.newItems.orEmpty())
//                }
//                Status.ERROR -> {
//                    mLoadingView.hide()
//                }
//                Status.LOADING -> {
//                    mLoadingView.show()
//                }
//            }
//        })

//        viewModel.videoList.observe(this, Observer { resource ->
//            when (resource.status) {
//                Status.SUCCESS -> {
//                    val hideMainLoadingView = (mListAdapter.itemCount == 0)
//                    lifecycleScope.launch {
//                        videoMetas = videoMetas + mainViewModel.getVideoMetas(resource.data.orEmpty())
//                        if (hideMainLoadingView) {
//                            mLoadingView.hide()
//                        } else {
//                            mListAdapter.setShowLoadingAtBottom(false)
//                        }
//                        mListAdapter.submitList(videoMetas)
//                    }
//                }
//                Status.ERROR -> {
//                    if (mListAdapter.itemCount == 0) {
//                        mLoadingView.hide()
//                    } else {
//                        mListAdapter.setShowLoadingAtBottom(false)
//                    }
//                }
//                Status.LOADING -> {
//                    if (mListAdapter.itemCount == 0) {
//                        mLoadingView.show()
//                    } else {
//                        mListAdapter.setShowLoadingAtBottom(true)
//                    }
//                }
//            }
//
//        })

        viewModel.isEditMode.observe(this, Observer {
            mOnBackPressedCallback.isEnabled = it
            mListAdapter.setEditMode(it)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_video_list, container, false)
            setupView()
        }
        return rootView
    }

    override fun onStart() {
        super.onStart()
        fetchVideoList()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setupView() {
        mToolbar = rootView!!.findViewById(R.id.toolbar)
        mVideoListView = rootView!!.findViewById(R.id.video_list)
        mLoadingView = rootView!!.findViewById(R.id.loading_view)
        mButton = rootView!!.findViewById(R.id.load_more_btn)

        mToolbar.setupWithNavController(findNavController())
        mVideoListView.apply {
            adapter = mListAdapter
            addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            layoutManager = LinearLayoutManager(requireContext())
        }
        mButton.setOnClickListener {
            viewModel.loadMoreVideos()
        }
        mButton.visibility = View.GONE
    }

    private fun fetchVideoList() {
        viewModel.setPlaylist(args.playlist)
    }

}
