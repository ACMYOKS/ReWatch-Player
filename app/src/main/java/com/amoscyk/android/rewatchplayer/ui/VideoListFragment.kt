package com.amoscyk.android.rewatchplayer.ui


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.ContentLoadingProgressBar
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
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMeta
import com.amoscyk.android.rewatchplayer.util.dpToPx
import com.amoscyk.android.rewatchplayer.viewModelFactory
import kotlinx.coroutines.launch

class VideoListFragment : ReWatchPlayerFragment() {

    private val mainViewModel by viewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<VideoListViewModel> { viewModelFactory }

    private var videoMetas = listOf<VideoMeta>()

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
                setEditMode(true)
                toggleItemSelection(position)
            }
            true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

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

        viewModel.playlistResource.observe(this, Observer { res ->
            when (res.status) {
                Status.SUCCESS -> {
                    mLoadingView.hide()
                    viewModel.setPlaylistItems(res.data.orEmpty())
                }
                Status.ERROR -> {
                    mLoadingView.hide()
                }
                Status.LOADING -> {
                    mLoadingView.show()
                }
            }
        })

        viewModel.videoList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    val hideMainLoadingView = (mListAdapter.itemCount == 0)
                    lifecycleScope.launch {
                        videoMetas = mainViewModel.getVideoMetas(resource.data.orEmpty())
                        if (hideMainLoadingView) {
                            mLoadingView.hide()
                        } else {
                            mListAdapter.setShowLoadingAtBottom(false)
                        }
                        mListAdapter.submitList(videoMetas)
                    }
                }
                Status.ERROR -> {
                    if (mListAdapter.itemCount == 0) {
                        mLoadingView.hide()
                    } else {
                        mListAdapter.setShowLoadingAtBottom(false)
                    }
                }
                Status.LOADING -> {
                    if (mListAdapter.itemCount == 0) {
                        mLoadingView.show()
                    } else {
                        mListAdapter.setShowLoadingAtBottom(true)
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
            addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        (layoutManager as? LinearLayoutManager)?.apply {
                            if (findLastCompletelyVisibleItemPosition() == itemCount - 1) {
                                mListAdapter.setShowLoadingAtBottom(true)
                                viewModel.loadMoreVideos()
                            }
                        }
                    }
                }
            })
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
