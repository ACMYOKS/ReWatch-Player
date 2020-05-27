package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.CommonListDecoration
import com.amoscyk.android.rewatchplayer.ui.PlaylistListAdapter
import com.amoscyk.android.rewatchplayer.ui.SubscriptionListAdapter
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar

class LibraryFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private lateinit var typeSpinner: Spinner
    private lateinit var loadPlaylistBtn: Button
    private lateinit var loadMoreBtn: Button
    private lateinit var channelList: RecyclerView
    private lateinit var playlistList: RecyclerView
    private lateinit var bookmarkList: RecyclerView
    private lateinit var loadingView: ContentLoadingProgressBar
    private val viewModel by viewModels<LibraryViewModel> { viewModelFactory }

    private val mOnBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.setEditMode(false)
        }
    }

    private var currentDisplayMode = LibraryViewModel.DisplayMode.CHANNEL
    private var isEditMode = false

    private lateinit var mTypeSpinnerAdapter: ListTypeAdapter
    private val mChannelListAdapter = SubscriptionListAdapter(onItemClick = {
        findNavController().navigate(LibraryFragmentDirections.showChannelDetail(it.channelId))
    })
    private val mPlaylistAdapter = PlaylistListAdapter(itemOnClick = {
        findNavController().navigate(LibraryFragmentDirections.showVideoListForPlaylist(it, true))
    })
    private val mBookmarkListAdapter = VideoListAdapter().apply {
        setArchivable(true)
        setOnArchiveClickListener { position, meta ->
            mainActivity?.showArchiveOption(meta.videoId)
        }
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
            } else {
                mainActivity?.playVideoForId(meta.videoId)
            }
        }
        setOnItemLongClickListener { position, meta ->
            viewModel.setEditMode(true)
            toggleItemSelection(position)
            true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().onBackPressedDispatcher.addCallback(mOnBackPressedCallback)

        mTypeSpinnerAdapter = ListTypeAdapter(context)

        viewModel.editMode.observe(this, Observer {
            isEditMode = it
            mOnBackPressedCallback.isEnabled = it
            setMenuItemVisibility(it)
            mBookmarkListAdapter.setEditMode(it)
        })
        viewModel.currentDisplayMode.observe(this, Observer {
            currentDisplayMode = it
            typeSpinner.setSelection(it.ordinal)
            setMenuItemVisibility(isEditMode)
            setListForDisplayMode(it)
        })
        viewModel.channelList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    if (mChannelListAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                    mChannelListAdapter.submitList(resource.data)
                }
                Status.ERROR -> {
                    if (mChannelListAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                    (resource.message as? Exception)?.let { exception ->
                        Log.d("LOG", exception.message ?: "has exception")
                    }
                }
                Status.LOADING -> {
                    if (mChannelListAdapter.itemCount == 0) {
                        loadingView.show()
                    }
                }
            }
        })
        viewModel.playlistList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    if (mPlaylistAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                    mPlaylistAdapter.submitList(resource.data)
                }
                Status.ERROR -> {
                    if (mPlaylistAdapter.itemCount == 0) {
                        loadingView.hide()
                    }
                    (resource.message as? Exception)?.let { exception ->
                        Log.d("LOG", exception.message ?: "has exception")
                    }
                }
                Status.LOADING -> {
                    if (mPlaylistAdapter.itemCount == 0) {
                        loadingView.show()
                    }
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
                    edit { putString(PreferenceKey.LIBRARY_LIST_MODE, currentDisplayMode.name) }
                else
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.valueOf(it))
            }
        }
        viewModel.getVideoList()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().appSharedPreference.apply {
            edit { putString(PreferenceKey.LIBRARY_LIST_MODE, currentDisplayMode.name) }
        }
    }

    private fun setupViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
        typeSpinner = rootView!!.findViewById(R.id.spinner_list_type)
        channelList = rootView!!.findViewById(R.id.channel_list)
        playlistList = rootView!!.findViewById(R.id.playlist_list)
        bookmarkList = rootView!!.findViewById(R.id.bookmark_list)
        loadPlaylistBtn = rootView!!.findViewById(R.id.load_playlist_btn)
        loadMoreBtn = rootView!!.findViewById(R.id.load_more_playlist_btn)
        loadingView = rootView!!.findViewById(R.id.loading_view)

        toolbar.setupWithNavController(findNavController())
        typeSpinner.apply {
            adapter = mTypeSpinnerAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.values()[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
        channelList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            adapter = mChannelListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        (layoutManager as? LinearLayoutManager)?.apply {
                            if (findLastCompletelyVisibleItemPosition() == itemCount - 1) {
                                viewModel.loadMoreChannels()
                            }
                        }
                    }
                }
            })
        }
        playlistList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
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
            addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
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
                R.id.refresh_list -> {
                    viewModel.refreshList()
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

    private fun setListForDisplayMode(displayMode: LibraryViewModel.DisplayMode) {
        when (displayMode) {
            LibraryViewModel.DisplayMode.CHANNEL -> {
                channelList.visibility = View.VISIBLE
                playlistList.visibility = View.INVISIBLE
                bookmarkList.visibility = View.INVISIBLE
            }
            LibraryViewModel.DisplayMode.PLAYLISTS -> {
                channelList.visibility = View.INVISIBLE
                playlistList.visibility = View.VISIBLE
                bookmarkList.visibility = View.INVISIBLE
            }
            LibraryViewModel.DisplayMode.BOOKMARKED -> {
                channelList.visibility = View.INVISIBLE
                playlistList.visibility = View.INVISIBLE
                bookmarkList.visibility = View.VISIBLE
            }
        }
    }

    private fun setMenuItemVisibility(isEditMode: Boolean) {
        with(toolbar.menu) {
            setGroupVisible(R.id.library_option_group, !isEditMode)
            setGroupVisible(R.id.library_edit_group, isEditMode)
        }
    }

    private class ListTypeAdapter(context: Context): ArrayAdapter<String>(context,
        R.layout.list_type_adapter_view, R.id.tv_type) {
        private val list = listOf(
            Pair(R.string.library_list_channel, R.drawable.ic_subscriptions_white),
            Pair(R.string.library_list_playlist, R.drawable.ic_view_list_white),
            Pair(R.string.library_list_bookmarked, R.drawable.ic_bookmark_border_white))

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): String? {
            return context.getString(list[position].first)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_type_adapter_view, parent, false)
            view.findViewById<ImageView>(R.id.iv_type).apply {
                setImageResource(list[position].second)
            }
            view.findViewById<TextView>(R.id.tv_type).apply {
                text = getItem(position)
            }
            return view
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }
    }
}
