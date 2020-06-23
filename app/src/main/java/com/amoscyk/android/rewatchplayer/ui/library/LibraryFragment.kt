package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
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
import com.amoscyk.android.rewatchplayer.ui.CommonListDecoration
import com.amoscyk.android.rewatchplayer.ui.PlaylistListAdapter
import com.amoscyk.android.rewatchplayer.ui.SubscriptionListAdapter
import com.amoscyk.android.rewatchplayer.ui.VideoListAdapter
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_library.view.*

class LibraryFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private var actionMode: ActionMode? = null
    private val toolbar get() = view!!.toolbar
    private val typeSpinner get() = view!!.spinner_list_type
    private val channelList get() = view!!.channel_list
    private val playlistList get() = view!!.playlist_list
    private val bookmarkList get() = view!!.bookmark_list
    private val loadPlaylistBtn get() = view!!.load_playlist_btn
    private val loadMoreBtn get() = view!!.load_more_playlist_btn
    private val loadingView get() = view!!.loading_view
    private val viewModel by viewModels<LibraryViewModel> { viewModelFactory }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.library_action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.confirm_delete -> {
                    viewModel.removeBookmark(mBookmarkListAdapter.getSelectedItemsId())
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (actionMode != null) actionMode = null
            viewModel.setEditMode(false)
        }

    }

    private var currentDisplayMode = LibraryViewModel.DisplayMode.CHANNEL
    private var isEditMode = false

    private lateinit var mTypeSpinnerAdapter: ListTypeAdapter
    private val mChannelListAdapter = SubscriptionListAdapter(onItemClick = {
        findNavController().navigate(LibraryFragmentDirections.showChannelDetail(it.channelId))
    }).apply {
        setOnLoadMoreNeeded { viewModel.loadMoreChannels() }
    }.apply {
        setHasStableIds(true)
    }
    private val mPlaylistAdapter = PlaylistListAdapter(itemOnClick = {
        findNavController().navigate(LibraryFragmentDirections.showVideoListForPlaylist(it, true))
    }).apply {
        setOnLoadMoreNeeded { viewModel.loadMorePlaylists() }
        setHasStableIds(true)
    }
    private val mBookmarkListAdapter = VideoListAdapter().apply {
        setArchivable(true)
        setOnArchiveClickListener { position, meta ->
            mainActivity?.showArchiveOption(meta.videoId)
        }
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
                actionMode?.title = getSelectedItemsId().size.toString()
            } else {
                mainActivity?.playVideoForId(meta.videoId)
            }
        }
        setOnItemLongClickListener { position, meta ->
            viewModel.setEditMode(true)
            toggleItemSelection(position)
            actionMode?.title = getSelectedItemsId().size.toString()
            true
        }
        setHasStableIds(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mTypeSpinnerAdapter = ListTypeAdapter(context)

        viewModel.editMode.observe(this, Observer {
            isEditMode = it
//            mOnBackPressedCallback.isEnabled = it
//            setMenuItemVisibility(it)
            mBookmarkListAdapter.setEditMode(it)
            if (it) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    actionModeCallback
                )
            } else {
                actionMode?.finish()
            }
        })
        viewModel.currentDisplayMode.observe(this, Observer {
            currentDisplayMode = it
            typeSpinner.setSelection(it.ordinal)
//            setMenuItemVisibility(isEditMode)
            setListForDisplayMode(it)
        })
        viewModel.channelList.observe(this, Observer {
            mChannelListAdapter.submitList(it.accumulatedItems + it.newItems)
            mChannelListAdapter.setEnableInfiniteLoad(!it.isEndOfList)
        })
        viewModel.playlistList.observe(this, Observer {
            mPlaylistAdapter.submitList(it.accumulatedItems + it.newItems)
            mPlaylistAdapter.setEnableInfiniteLoad(!it.isEndOfList)
        })
        viewModel.bookmarkList.observe(this, Observer {
            mBookmarkListAdapter.submitList(it.map { it.videoMeta })
        })
        viewModel.showLoadingChannel.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        channelList.visibility = View.INVISIBLE
                        loadingView.show()
                    } else {
                        channelList.visibility = View.VISIBLE
                        loadingView.hide()
                    }
                }
            }
        })
        viewModel.showLoadingPlaylist.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        playlistList.visibility = View.INVISIBLE
                        loadingView.show()
                    } else {
                        playlistList.visibility = View.VISIBLE
                        loadingView.hide()
                    }
                }
            }
        })
        viewModel.showLoadingBookmarked.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        bookmarkList.visibility = View.INVISIBLE
                        loadingView.show()
                    } else {
                        bookmarkList.visibility = View.VISIBLE
                        loadingView.hide()
                    }
                }
            }
        })
        viewModel.bookmarkRemoveCount.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                Snackbar.make(
                    view!!,
                    getString(R.string.library_bookmark_removed, it),
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.setEditMode(false)
                viewModel.refreshList()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupOptionMenu()
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
        }
        playlistList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            adapter = mPlaylistAdapter
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
//        toolbar.inflateMenu(R.menu.library_option_menu)
//        toolbar.setOnMenuItemClickListener {
//            when (it.itemId) {
//                R.id.refresh_list -> {
//                    viewModel.refreshList()
//                }
//                R.id.edit_item -> {
//                    viewModel.setEditMode(true)
//                }
//                R.id.confirm_delete -> {
//                    viewModel.setEditMode(false)
//                }
//            }
//            true
//        }
        toolbar.apply {
            inflateMenu(R.menu.library_option_menu2)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.refresh_list -> {
                        viewModel.refreshList()
                    }
                }
                true
            }
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

//    private fun setMenuItemVisibility(isEditMode: Boolean) {
//        with(toolbar.menu) {
//            setGroupVisible(R.id.library_option_group, !isEditMode)
//            setGroupVisible(R.id.library_edit_group, isEditMode)
//        }
//    }

    private class ListTypeAdapter(context: Context): ArrayAdapter<String>(context,
        R.layout.list_type_adapter_view, R.id.tv_type) {
        private val list = listOf(
            Pair(R.string.library_list_channel, R.drawable.ic_subscriptions_white),
            Pair(R.string.library_list_playlist, R.drawable.ic_view_list_white),
            Pair(R.string.library_list_bookmarked, R.drawable.ic_bookmark_white)
        )

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
