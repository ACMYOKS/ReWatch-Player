package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.CommonListDecoration
import com.amoscyk.android.rewatchplayer.ui.PlaylistListAdapter
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getString
import com.amoscyk.android.rewatchplayer.util.putString
import com.amoscyk.android.rewatchplayer.viewModelFactory

class LibraryFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private lateinit var loadPlaylistBtn: Button
    private lateinit var loadMoreBtn: Button
    private lateinit var playlistList: RecyclerView
    private lateinit var bookmarkList: RecyclerView
    private lateinit var loadingView: ContentLoadingProgressBar
    private val viewModel by viewModels<LibraryViewModel> { viewModelFactory }

    private val mPlaylistAdapter = PlaylistListAdapter(itemOnClick = {
        findNavController().navigate(LibraryFragmentDirections.showVideoList(it))
    })
    private val mBookmarkListAdapter = VideoListAdapter(onItemClick = {
        mainActivity?.playVideoForId(it.videoId)
    })

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.editMode.observe(this, Observer { isEditMode ->
            setMenuItemVisibility(isEditMode, viewModel.currentDisplayMode.value!!)
        })
        viewModel.currentDisplayMode.observe(this, Observer {
            setMenuItemVisibility(viewModel.editMode.value!!, it)
            setListForDisplayMode(it)
        })
        viewModel.playlistList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    loadingView.hide()
                    mPlaylistAdapter.submitList(resource.data)
                }
                Status.ERROR -> {
                    loadingView.hide()
                    (resource.message as? Exception)?.let { exception ->
                        Log.d("LOG", exception.message ?: "has exception")
                    }
                }
                Status.LOADING -> {
                    loadingView.show()
                }
            }
        })
        viewModel.bookmarkList.observe(this, Observer { res ->
            when (res.status) {
                Status.SUCCESS -> {
                    if (mBookmarkListAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                    mBookmarkListAdapter.submitList(res.data?.map { it.videoMeta })
                }
                Status.ERROR -> {
                    if (mBookmarkListAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                }
                Status.LOADING -> {
                    if (mBookmarkListAdapter.itemCount == 0) {
                        loadingView.show()
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
            // Inflate the layout for this fragment
            rootView = inflater.inflate(R.layout.fragment_library, container, false)
            setupViews()
            setupOptionMenu()
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        requireActivity().appSharedPreference.apply {
            getString(PreferenceKey.LIBRARY_LIST_MODE, null).let {
                if (it == null)
                    edit { putString(PreferenceKey.LIBRARY_LIST_MODE, viewModel.currentDisplayMode.value?.name) }
                else
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.valueOf(it))
            }
        }
        reloadList()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().appSharedPreference.apply {
            edit { putString(PreferenceKey.LIBRARY_LIST_MODE, viewModel.currentDisplayMode.value?.name) }
        }
    }

    private fun setupViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
        playlistList = rootView!!.findViewById(R.id.playlist_list)
        bookmarkList = rootView!!.findViewById(R.id.bookmark_list)
        loadPlaylistBtn = rootView!!.findViewById(R.id.load_playlist_btn)
        loadMoreBtn = rootView!!.findViewById(R.id.load_more_playlist_btn)
        loadingView = rootView!!.findViewById(R.id.loading_view)

        toolbar.setupWithNavController(findNavController())
        playlistList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(8, 8))
            adapter = mPlaylistAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        (layoutManager as? LinearLayoutManager)?.apply {
                            if (findLastCompletelyVisibleItemPosition() == itemCount - 1) {
                                viewModel.loadMorePlaylists()
                            }
                        }
                    }
                }
            })
        }
        bookmarkList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(8, 8))
            adapter = mBookmarkListAdapter
        }
//        loadPlaylistBtn.setOnClickListener {
//            if (viewModel.currentDisplayMode.value == LibraryViewModel.DisplayMode.PLAYLISTS) {
//                viewModel.loadPlaylists()
//            } else {
//                viewModel.loadBookmarkList()
//            }
//        }
//        loadMoreBtn.setOnClickListener {
//            viewModel.loadMorePlaylists()
//        }
        loadingView.hide()
    }

    private fun setupOptionMenu() {
        toolbar.inflateMenu(R.menu.library_option_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
//                R.id.search_item -> {
//
//                }
                R.id.show_bookmark_list -> {
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.BOOKMARKED)
                }
                R.id.show_playlists -> {
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.PLAYLISTS)
                }
                R.id.edit_item -> {
                    viewModel.setEditMode(true)
                }
                R.id.confirm_delete -> {
                    viewModel.setEditMode(false)
                }
            }
            true
        }
    }

    private fun reloadList() {
        viewModel.loadPlaylists()
        viewModel.loadBookmarkList()
    }

    private fun setListForDisplayMode(displayMode: LibraryViewModel.DisplayMode) {
        when (displayMode) {
            LibraryViewModel.DisplayMode.PLAYLISTS -> {
                playlistList.visibility = View.VISIBLE
                bookmarkList.visibility = View.INVISIBLE
            }
            LibraryViewModel.DisplayMode.BOOKMARKED -> {
                playlistList.visibility = View.INVISIBLE
                bookmarkList.visibility = View.VISIBLE
            }
            LibraryViewModel.DisplayMode.SAVED -> {
                playlistList.visibility = View.INVISIBLE
                bookmarkList.visibility = View.INVISIBLE
            }
        }
    }

    private fun setMenuItemVisibility(isEditMode: Boolean, displayMode: LibraryViewModel.DisplayMode) {
        with(toolbar.menu) {
            setGroupVisible(R.id.library_option_group, !isEditMode)
            setGroupVisible(R.id.library_edit_group, isEditMode)
            if (!isEditMode) {
                findItem(R.id.show_bookmark_list).isVisible = displayMode == LibraryViewModel.DisplayMode.PLAYLISTS
                findItem(R.id.show_playlists).isVisible = displayMode == LibraryViewModel.DisplayMode.BOOKMARKED
            }
        }
    }

}
