package com.amoscyk.android.rewatchplayer.ui.home


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.RPVideo
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.MainActivity
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException

class VideoSearchFragment : ReWatchPlayerFragment() {

    enum class SearchCriteria(val displayName: String) {
        BY_ID("by ID"),
        BY_TITLE("by title")
    }

    private var mRootView: View? = null
    private lateinit var mSuggestionList: RecyclerView
    private lateinit var mVideoList: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var mSearchView: SearchView
    private lateinit var mTextView: TextView
    private lateinit var mLoadBtn: Button
    private lateinit var mSpinner: AppCompatSpinner
    private lateinit var mLoadingView: ProgressBar

    private val mVideoListAdapter = VideoListAdapter()

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<VideoSearchViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "video search on create")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.searchResults.observe(this, Observer { resource ->
            when (resource.status) {
                Status.LOADING -> {

                }
                Status.SUCCESS -> {
                    // FIXME: get video from video api instead of direct conversion
                    resource.data!!
                        .map { searchResult -> searchResult.toRPVideo() }
                        .let { list -> mVideoListAdapter.submitList(list.map { it.toVideoMeta() }) }
                }
                Status.ERROR -> {
                    (resource.message as? UserRecoverableAuthIOException)?.let { exception ->
                        requestForGoogleUserAuth(exception)
                    }
                }
            }
        })
        viewModel.videoExists.observe(this, Observer { resource ->
            when (resource.status) {
                Status.LOADING -> {
                    mLoadingView.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "checking", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    mLoadingView.visibility = View.GONE
                    if (resource.data!!.second) {
                        mainActivity?.apply {
                            playVideoForId(resource.data.first)
                        }
                    } else {
                        Toast.makeText(requireContext(),
                            "Video with id: ${resource.data.first} not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                Status.ERROR -> {
                    mLoadingView.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_video_search, container, false)
            setupViews()
        }
        return mRootView
    }

    override fun onGoogleUserAuthResult(resultCode: Int) {
        super.onGoogleUserAuthResult(resultCode)

    }

    private fun setupViews() {
        mSuggestionList = mRootView!!.findViewById(R.id.suggestion_list)
        mVideoList = mRootView!!.findViewById(R.id.video_list)
        mToolbar = mRootView!!.findViewById(R.id.toolbar)
        mSearchView = mRootView!!.findViewById(R.id.search_view)
        mTextView = mRootView!!.findViewById(R.id.test_tv)
        mLoadBtn = mRootView!!.findViewById(R.id.loadmorebtn)
        mSpinner = mRootView!!.findViewById(R.id.search_type_spinner)
        mLoadingView = mRootView!!.findViewById(R.id.loading_view)

        mToolbar.setupWithNavController(findNavController())
        mSearchView.apply {
            setOnSearchClickListener {
                Log.d("TAG", "search view on click")
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
//                    Log.d("TAG", "query text changed: $newText")
                    return false
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d("TAG", "query text submit: $query")
                    query?.let {
                        when (mSpinner.selectedItemPosition) {
                            SearchCriteria.BY_ID.ordinal -> {
//                                viewModel.searchForVideoId(it)
                                mainViewModel.playVideoForId(requireContext(), it, true)
                            }
                            SearchCriteria.BY_TITLE.ordinal -> {
                                viewModel.searchForQuery(it)
                            }
                        }
                    }
                    return false
                }
            })
            setOnCloseListener {
                Log.d("TAG", "search view on close")
                false
            }
        }
        mLoadBtn.setOnClickListener {
            viewModel.loadMoreResource()
        }
        mSpinner.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            SearchCriteria.values().map { it.displayName })

        mVideoList.adapter = mVideoListAdapter
        mVideoList.layoutManager = LinearLayoutManager(requireContext())
    }

    companion object {
        const val TAG = "VideoSearchFragment"
    }

}
