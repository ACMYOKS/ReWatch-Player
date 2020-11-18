package com.amoscyk.android.rewatchplayer.ui.home


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.ListLoadingState
import com.amoscyk.android.rewatchplayer.ui.CommonListDecoration
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.ui.viewcontrol.SnackbarControl
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
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
    private val mLoadingView: ContentLoadingProgressBar get() = view!!.loading_view
    private val mEmptyView: View get() = view!!.empty_list_view

    private val args by navArgs<VideoSearchFragmentArgs>()
    private var handleVideoId: String? = null

    private val mVideoListAdapter = VideoListAdapter().apply {
        setOnLoadMoreNeeded {
            viewModel.loadMoreResource()
        }
        setOnItemClickListener { position, meta ->
            mainViewModel.readyVideo(meta.videoId)
        }
        setArchivable(true)
        setBookmarkable(true)
        setOnArchiveClickListener { position, meta ->
            mainViewModel.readyArchive(meta.videoId)
        }
        setOnBookmarkClickListener { position, meta ->
            mainViewModel.setBookmarked(
                meta.videoId,
                meta.videoId !in mainViewModel.bookmarkedVid.value.orEmpty()
            )
        }
    }
    private val snackbarSet = hashSetOf<Snackbar>()

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<VideoSearchViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleVideoId = args.videoId
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        handleGoogleUserAuthEvent(viewModel)

        viewModel.showLoading.observe(this, Observer { event ->
            event.getContentIfNotHandled { content: ListLoadingState ->
                if (!content.loadMore) {
                    if (content.isLoading) {
                        mEmptyView.visibility = View.GONE
                        mLoadingView.show()
                    } else {
                        mLoadingView.hide()
                    }
                }
            }
        })
        viewModel.searchList.observe(this, Observer {
            mEmptyView.visibility = if ((it.accumulatedItems + it.newItems).isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            mVideoListAdapter.submitList(it.accumulatedItems + it.newItems)
            mVideoListAdapter.setEnableInfiniteLoad(!it.isEndOfList)
        })
        viewModel.errorEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                AlertDialog.Builder(requireContext())
                    .setMessage(it)
                    .setTitle(R.string.player_error_title)
                    .setPositiveButton(R.string.confirm_text) { d, i -> d.cancel() }
                    .create()
                    .show()
            }
        })
        viewModel.snackEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled { ctrl ->
                mainFragment?.newSnackbar(ctrl.title, when (ctrl.duration) {
                    SnackbarControl.Duration.SHORT -> Snackbar.LENGTH_SHORT
                    SnackbarControl.Duration.LONG -> Snackbar.LENGTH_LONG
                    else -> Snackbar.LENGTH_INDEFINITE
                }
                )?.also {
                    if (ctrl.action != null) {
                        it.setAction(ctrl.action.title) { ctrl.action.action() }
                    }
                    it.addCallback(object : Snackbar.Callback() {
                        override fun onShown(sb: Snackbar?) {
                            sb?.let { snackbarSet.add(it) }
                        }

                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            transientBottomBar?.let { snackbarSet.remove(it) }
                        }
                    })
                    it.show()
                }
            }
        })
        mainViewModel.bookmarkedVid.observe(this, Observer {
            mVideoListAdapter.setBookmarkedVideoId(it)
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

    override fun onStart() {
        super.onStart()
        handleVideoId?.let {
            mSearchView.isIconified = false
            mSearchView.setQuery(it, true)
            handleVideoId = null
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().appSharedPreference.edit {
            putInt(PreferenceKey.SEARCH_OPTION, getSelectedSearchOptionPos())
        }
        snackbarSet.forEach {
            it.dismiss()
        }
        snackbarSet.clear()
    }

    private fun setupViews() {
        mToolbar.apply {
            setupWithNavController(findNavController())
            // disable menu
            /*inflateMenu(R.menu.search_option_menu)
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
            }*/
        }
        mSearchView.apply {
            setIconifiedByDefault(false)
            setOnSearchClickListener {

            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }

                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let {
                        getSelectedSearchOptionPos().let { pos ->
                            if (pos == 0) {
                                viewModel.searchForQuery(it)
                            } else {
                                viewModel.searchForVideoId(it)
                            }
                        }
                    }
                    return false
                }
            })
            setOnCloseListener {
                false
            }
        }

        mVideoList.adapter = mVideoListAdapter
        mVideoList.layoutManager = LinearLayoutManager(requireContext())
        mVideoList.addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(4f).toInt()))
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
