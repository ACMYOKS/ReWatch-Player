package com.amoscyk.android.rewatchplayer.ui


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
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
import com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity
import com.amoscyk.android.rewatchplayer.viewModelFactory

class VideoListFragment : ReWatchPlayerFragment() {

    private val viewModel by viewModels<VideoListViewModel> { viewModelFactory }

    private val args by navArgs<VideoListFragmentArgs>()
    private var rootView: View? = null
    private lateinit var mToolbar: Toolbar
    private lateinit var mVideoListView: RecyclerView
    private lateinit var mLoadingView: ProgressBar
    private lateinit var mButton: Button
    private val mListAdapter = VideoListAdapter(onItemClick = {
        Toast.makeText(requireContext(), "${it.title} ${it.channelTitle}", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_VIDEO_ID, it.id)
        startActivity(intent)
    })

    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModel.title.observe(this, Observer { title ->
            mToolbar.title = title
        })

        viewModel.videoList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    mLoadingView.visibility = View.GONE
                    mListAdapter.submitList(resource.data)
                }
                Status.ERROR -> {
                    mLoadingView.visibility = View.GONE
                }
                Status.LOADING -> {
                    mLoadingView.visibility = View.VISIBLE
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

    override fun onResume() {
        super.onResume()
        fetchVideoList()
    }

    private fun setupView() {
        mToolbar = rootView!!.findViewById(R.id.toolbar)
        mVideoListView = rootView!!.findViewById(R.id.video_list)
        mLoadingView = rootView!!.findViewById(R.id.loading_view)
        mButton = rootView!!.findViewById(R.id.load_more_btn)

        mToolbar.setupWithNavController(findNavController())
        mVideoListView.adapter = mListAdapter
        mVideoListView.addItemDecoration(CommonListDecoration(4))
        mVideoListView.layoutManager = LinearLayoutManager(requireContext())
        mButton.setOnClickListener {
            viewModel.loadMoreVideos()
        }
    }

    private fun fetchVideoList() {
        viewModel.setPlaylist(args.playlist)
    }

}
