package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.NoNetworkException
import com.amoscyk.android.rewatchplayer.datasource.vo.ServerErrorException
import com.amoscyk.android.rewatchplayer.ui.*
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_library.view.*
import java.net.SocketTimeoutException

class LibraryFragment : ReWatchPlayerFragment() {

    private var actionMode: ActionMode? = null
    private val toolbar get() = view!!.toolbar
    private val viewPager get() = view!!.view_pager
    private val tabLayout get() = view!!.tab_layout
    private lateinit var channelFrag: ContentListFragment
    private lateinit var playlistFrag: ContentListFragment
    private lateinit var bookmarkFrag: ContentListFragment
    private lateinit var historyFrag: ContentListFragment
    private var indefiniteSb: Snackbar? = null      // special ref for snackbar with indefinite show duration
    private val viewModel by viewModels<LibraryViewModel> { viewModelFactory }
    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }

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
                    if (currentDisplayMode == LibraryViewModel.DisplayMode.BOOKMARKED) {
                        viewModel.removeBookmark(mBookmarkListAdapter.getSelectedItemsId())
                    } else if (currentDisplayMode == LibraryViewModel.DisplayMode.HISTORY) {
                        viewModel.removeWatchHistory(mHistoryListAdapter.getSelectedItemsId())
                    }
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
            mainViewModel.readyArchive(meta.videoId)
        }
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
                actionMode?.title = getSelectedItemsId().size.toString()
            } else {
//                mainActivity?.playVideoForId(meta.videoId)
                mainViewModel?.readyVideo(meta.videoId)
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
    private val mHistoryListAdapter = WatchHistoryListAdapter().apply {
        setOnItemClickListener { position, meta ->
            if (isEditMode) {
                toggleItemSelection(position)
                actionMode?.title = getSelectedItemsId().size.toString()
            } else {
//                mainActivity?.playVideoForId(meta.videoId)
                mainViewModel.readyVideo(meta.videoId)
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

        mainViewModel.currentAccountName.observe(this, Observer {
            viewModel.refreshList()
        })
        viewModel.editMode.observe(this, Observer {
            isEditMode = it
            if (currentDisplayMode == LibraryViewModel.DisplayMode.BOOKMARKED) {
                mBookmarkListAdapter.setEditMode(it)
            } else if (currentDisplayMode == LibraryViewModel.DisplayMode.HISTORY) {
                mHistoryListAdapter.setEditMode(it)
            }
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
            setListForDisplayMode(it)
            if (indefiniteSb != null) {
                indefiniteSb!!.dismiss()
                indefiniteSb = null
            }
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
        viewModel.historyList.observe(this, Observer {
            mHistoryListAdapter.submitList(it)
        })
        viewModel.showLoadingChannel.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        channelFrag.beginLoadingView()
                    } else {
                        channelFrag.endLoadingView()
                    }
                }
            }
        })
        viewModel.showLoadingPlaylist.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        playlistFrag.beginLoadingView()
                    } else {
                        playlistFrag.endLoadingView()
                    }
                }
            }
        })
        viewModel.showLoadingBookmarked.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        bookmarkFrag.beginLoadingView()
                    } else {
                        bookmarkFrag.endLoadingView()
                    }
                }
            }
        })
        viewModel.showLoadingHistory.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                if (!it.loadMore) {
                    if (it.isLoading) {
                        historyFrag.beginLoadingView()
                    } else {
                        historyFrag.endLoadingView()
                    }
                }
            }
        })
        viewModel.bookmarkRemoveCount.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                mainFragment?.showSnackbar(
                    getString(R.string.library_bookmark_removed, it),
                    Snackbar.LENGTH_SHORT
                )
                viewModel.setEditMode(false)
                viewModel.refreshList()
            }
        })

        viewModel.historyRemoveCount.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                mainFragment?.showSnackbar(
                    getString(R.string.library_history_removed, it),
                    Snackbar.LENGTH_SHORT
                )
                viewModel.setEditMode(false)
                viewModel.refreshList()
            }
        })

        viewModel.exceptionEvent.observe(this, Observer { event ->
            event.getContentIfNotHandled {
                val e = it.e
                val actionTag = it.actionTag
                when (e) {
                    is SocketTimeoutException -> {
                        mainFragment?.apply {
                            indefiniteSb =
                                newSnackbar(R.string.error_loading_resource, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.retry) {
                                        when (actionTag) {
                                            "refresh_list" -> viewModel.refreshList()
                                            "load_more_channels" -> viewModel.loadMoreChannels()
                                            "load_more_playlists" -> viewModel.loadMorePlaylists()
                                        }
                                    }
                            indefiniteSb!!.show()
                        }
                    }
                    is NoNetworkException -> {
                        mainFragment?.apply {
                            indefiniteSb =
                                newSnackbar(R.string.error_network_disconnected, Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.retry) {
                                        when (actionTag) {
                                            "refresh_list" -> viewModel.refreshList()
                                            "load_more_channels" -> viewModel.loadMoreChannels()
                                            "load_more_playlists" -> viewModel.loadMorePlaylists()
                                        }
                                    }
                            indefiniteSb!!.show()
                        }
                    }
                    else -> {
                        mainFragment?.newSnackbar(e.message ?: getString(R.string.error_unknown), Snackbar.LENGTH_LONG)
                            ?.show()
                    }
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         restore sub-fragments that are added to fragmentManager by ViewPager after instance
         of this fragment is saved; getting sub-fragment instance is needed in order to bind/rebind
         RecyclerView.Adapter from this fragment
         */
        channelFrag = getContentListFragment(0)
        playlistFrag = getContentListFragment(1)
        bookmarkFrag = getContentListFragment(2)
        historyFrag = getContentListFragment(3)
        channelFrag.setupRecyclerView {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            it.adapter = mChannelListAdapter
        }
        playlistFrag.setupRecyclerView {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            it.adapter = mPlaylistAdapter
        }
        bookmarkFrag.setupRecyclerView {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            it.adapter = mBookmarkListAdapter
        }
        historyFrag.setupRecyclerView {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.addItemDecoration(CommonListDecoration(dpToPx(4f).toInt(), dpToPx(8f).toInt()))
            it.adapter = mHistoryListAdapter
        }
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
        val displayMode = requireActivity().appSharedPreference.run {
            getString(PreferenceKey.LIBRARY_LIST_MODE, null).let {
                if (it == null) {
                    edit { putString(PreferenceKey.LIBRARY_LIST_MODE, DEFAULT_DISPLAY_MODE) }
                    DEFAULT_DISPLAY_MODE
                } else it
            }
        }
        viewModel.setDisplayMode(LibraryViewModel.DisplayMode.valueOf(displayMode))
        viewModel.getList()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().appSharedPreference.apply {
            edit { putString(PreferenceKey.LIBRARY_LIST_MODE, currentDisplayMode.name) }
        }
        if (indefiniteSb != null) {
            indefiniteSb!!.dismiss()
            indefiniteSb = null
        }
    }

    private fun getContentListFragment(position: Int): ContentListFragment =
        childFragmentManager.findFragmentByTag("android:switcher:${R.id.view_pager}:$position") as? ContentListFragment
            ?: ContentListFragment()

    private fun setupViews() {
        toolbar.setupWithNavController(findNavController())
        viewPager.apply {
            adapter = object :
                FragmentPagerAdapter(
                    childFragmentManager,
                    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                ) {
                override fun getCount(): Int = 4
                override fun getItem(position: Int): Fragment {
                    return when (position) {
                        0 -> channelFrag
                        1 -> playlistFrag
                        2 -> bookmarkFrag
                        else -> historyFrag
                    }
                }

                override fun getPageTitle(position: Int): CharSequence? {
                    return when (position) {
                        0 -> getString(R.string.library_list_channel)
                        1 -> getString(R.string.library_list_playlist)
                        2 -> getString(R.string.library_list_bookmarked)
                        else -> getString(R.string.library_list_watch_history)
                    }
                }
            }
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {

                }

                override fun onPageSelected(position: Int) {
                    viewModel.setEditMode(false)
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.values()[position])
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                }
            })
            offscreenPageLimit = 4
        }
        tabLayout.setupWithViewPager(viewPager)
    }

    private fun setupOptionMenu() {
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
        viewPager.currentItem = when (displayMode) {
            LibraryViewModel.DisplayMode.CHANNEL -> 0
            LibraryViewModel.DisplayMode.PLAYLISTS -> 1
            LibraryViewModel.DisplayMode.BOOKMARKED -> 2
            LibraryViewModel.DisplayMode.HISTORY -> 3
        }
    }

    companion object {
        private val DEFAULT_DISPLAY_MODE = LibraryViewModel.DisplayMode.PLAYLISTS.name
    }
}
