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
import androidx.recyclerview.widget.SimpleItemAnimator
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.NoNetworkException
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.util.dpToPx
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

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
            mainViewModel.readyArchive(meta.videoId)
        }
        setOnBookmarkClickListener { position, meta ->
            mainViewModel.setBookmarked(meta.videoId, !isBookmarked(position))
//            mainViewModel.toggleBookmarkStatus(meta.videoId)
        }
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
            } else {
//                mainActivity?.playVideoForId(meta.videoId)
                mainViewModel.readyVideo(meta.videoId)
            }
        }
//        setOnItemLongClickListener { position, meta ->
//            if (args.enableEditOnLongClick) {
//                viewModel.setEditMode(true)
//                toggleItemSelection(position)
//            }
//            true
//        }
        setOnLoadMoreNeeded {
            viewModel.loadMoreVideos()
        }
    }
    private var snackbar: Snackbar? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(mOnBackPressedCallback)

        mainViewModel.bookmarkedVid.observe(this, Observer {
            mListAdapter.setBookmarkedVideoId(it)
        })

        viewModel.title.observe(this, Observer { title ->
            mToolbar.title = title
        })

        viewModel.showListItemLoading.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        mLoadingView.show()
                    } else {
                        mLoadingView.hide()
                    }
                }
            }
        })

        viewModel.showVideoLoading.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        mLoadingView.show()
                    } else {
                        mLoadingView.hide()
                    }
                }
            }
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

        viewModel.isEditMode.observe(this, Observer {
            mOnBackPressedCallback.isEnabled = it
            mListAdapter.setEditMode(it)
        })

        viewModel.exceptionEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                when (it.e) {
                    is SocketTimeoutException -> {
                        mainFragment?.apply {
                            snackbar = newSnackbar(
                                R.string.error_loading_resource,
                                Snackbar.LENGTH_INDEFINITE
                            ).setAction(R.string.retry) {
                                viewModel.setPlaylist(args.playlist, true)
                            }.apply { show() }
                        }
                    }
                    is NoNetworkException -> {
                        mainFragment?.apply {
                            snackbar = newSnackbar(
                                R.string.error_network_disconnected,
                                Snackbar.LENGTH_INDEFINITE
                            ).setAction(R.string.retry) {
                                viewModel.setPlaylist(args.playlist, true)
                            }.apply { show() }
                        }
                    }
                }
            }
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
            addItemDecoration(CommonListDecoration(dpToPx(8f).toInt(), dpToPx(14f).toInt()))
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
