package com.amoscyk.android.rewatchplayer.ui.home


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getInt
import com.amoscyk.android.rewatchplayer.util.putInt
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.android.synthetic.main.fragment_video_search.*
import kotlinx.android.synthetic.main.fragment_video_search.view.*

class VideoSearchFragment : ReWatchPlayerFragment() {

    enum class SearchCriteria(val displayName: String) {
        BY_ID("by ID"),
        BY_TITLE("by title")
    }

    private val mSuggestionList: RecyclerView get() = view!!.suggestion_list
    private val mVideoList: RecyclerView get() = view!!.video_list
    private val mToolbar: Toolbar get() = view!!.toolbar
    private val mSearchView: SearchView get() = view!!.search_view
    private val mTextView: TextView get() = view!!.test_tv
    private val mLoadBtn: Button get() = view!!.loadmorebtn
    private val mLoadingView: ProgressBar get() = view!!.loading_view

    private val mVideoListAdapter = VideoListAdapter()

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<VideoSearchViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return inflater.inflate(R.layout.fragment_video_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onStop() {
        super.onStop()
        requireContext().appSharedPreference.edit {
            putInt(PreferenceKey.SEARCH_OPTION, getSelectedSearchOptionPos())
        }
    }

    override fun onGoogleUserAuthResult(resultCode: Int) {
        super.onGoogleUserAuthResult(resultCode)

    }

    private fun setupViews() {
        mToolbar.apply {
            setupWithNavController(findNavController())
            inflateMenu(R.menu.search_option_menu)
            val searchOptionPos =
                requireContext().appSharedPreference.getInt(PreferenceKey.SEARCH_OPTION, 0)
            menu[searchOptionPos].isChecked = true
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.search_by_title -> {
                        it.isChecked = true
                    }
                    R.id.search_by_id -> {
                        it.isChecked = true
                    }
                }
                true
            }
        }
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
//                        when (mSpinner.editText?.toString()) {
//                            SearchCriteria.BY_ID.name -> {
////                                viewModel.searchForVideoId(it)
//                                mainViewModel.playVideoForId(requireContext(), it, true)
//                            }
//                            SearchCriteria.BY_TITLE.name -> {
//                                viewModel.searchForQuery(it)
//                            }
//                        }
                        getSelectedSearchOptionPos().let { pos ->
                            if (pos == 0) {
                                viewModel.searchForQuery(it)
                            } else {
                                mainViewModel.playVideoForId(requireContext(), it, true)
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
//        mDropdown.setAdapter(
//            ArrayAdapter(requireContext(),
//                R.layout.dropdown_menu_popup_item,
//                SearchCriteria.values().map { it.displayName })
//        )

        mVideoList.adapter = mVideoListAdapter
        mVideoList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun getSelectedSearchOptionPos(): Int {
        toolbar.menu.forEachIndexed { index, item ->
            if (item.isChecked) return index
        }
        return -1
    }

    companion object {
        const val TAG = "VideoSearchFragment"
    }

}
