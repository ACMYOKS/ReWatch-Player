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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.viewModelFactory

class VideoListFragment : ReWatchPlayerFragment() {

    private val viewModel by viewModels<VideoListViewModel> { viewModelFactory }

    private val args by navArgs<VideoListFragmentArgs>()
    private var rootView: View? = null
    private lateinit var mToolbar: Toolbar
    private lateinit var mVideoListView: RecyclerView
    private lateinit var mLoadingView: ContentLoadingProgressBar
    private lateinit var mButton: Button
    private val mListAdapter = VideoListAdapter(onItemClick = {
        mainActivity?.playVideoForId(it.videoId)
    })

    override fun onAttach(context: Context) {
        super.onAttach(context)

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
                    if (mListAdapter.itemCount == 0) {
                        mLoadingView.hide()
                    } else {
                        mListAdapter.setShowLoadingAtBottom(false)
                    }
                    mListAdapter.submitList(resource.data?.map { it.toVideoMeta() })
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
            addItemDecoration(CommonListDecoration(4, 8))
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
    }

    private fun fetchVideoList() {
        viewModel.setPlaylist(args.playlist)
    }

}
