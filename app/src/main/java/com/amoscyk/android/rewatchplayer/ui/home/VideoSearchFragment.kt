package com.amoscyk.android.rewatchplayer.ui.home


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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
import com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity
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

    private val mVideoListAdapter = VideoListAdapter()

    private val viewModel by viewModels<VideoSearchViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "video search on create")
//        val intent = Intent(requireContext(), PlayerActivity::class.java)
//        intent.putExtra(PlayerActivity.EXTRA_VIDEO_ID, "")
//        startActivity(intent)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.searchResults.observe(this, Observer { resource ->
            when (resource.status) {
                Status.LOADING -> {

                }
                Status.SUCCESS -> {
//                    var string = ""
//                    it.data!!.forEach { result ->
//                        string += "title:${result.title} videoId:${result.videoId}\n"
//                    }
//                    mTextView.text = string
                    resource.data!!.map { searchResult ->
                        RPVideo.fromSearchResult(searchResult)
                    }.let {
                        mVideoListAdapter.submitList(it)
                    }
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
                    Toast.makeText(requireContext(), "checking", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    if (resource.data!!.second) {
                        val intent = Intent(requireContext(), PlayerActivity::class.java)
                        intent.putExtra(PlayerActivity.EXTRA_VIDEO_ID, resource.data.first)
                        startActivity(intent)
                    } else {
                        Toast.makeText(requireContext(),
                            "Video with id: ${resource.data.first} not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                Status.ERROR -> {
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
                                viewModel.searchForVideoId(it)
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
