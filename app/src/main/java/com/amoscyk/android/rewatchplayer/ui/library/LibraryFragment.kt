package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
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
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.ui.*
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_library.view.*

class LibraryFragment : ReWatchPlayerFragment() {

    private var actionMode: ActionMode? = null
    private val toolbar get() = view!!.toolbar
    private val typeSpinner get() = view!!.spinner_list_type
    private val viewPager get() = view!!.view_pager
    private lateinit var channelFrag: ContentListFragment
    private lateinit var playlistFrag: ContentListFragment
    private lateinit var bookmarkFrag: ContentListFragment
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

        mainViewModel.currentAccountName.observe(this, Observer {
            viewModel.refreshList()
        })
        viewModel.editMode.observe(this, Observer {
            isEditMode = it
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

        mainViewModel.bookmarkedVid.observe(this, Observer {
            // if bookmark count changed, refresh list
            viewModel.notifyBookmarkChanged()
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
                }
                else it
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
    }

    private fun getContentListFragment(position: Int): ContentListFragment =
        childFragmentManager.findFragmentByTag("android:switcher:${R.id.view_pager}:$position") as? ContentListFragment
            ?: ContentListFragment()

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
        viewPager.apply {
            adapter = object :
                FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
                override fun getCount(): Int = 3
                override fun getItem(position: Int): Fragment {
                    return when (position) {
                        0 -> channelFrag
                        1 -> playlistFrag
                        else -> bookmarkFrag
                    }
                }
            }
            offscreenPageLimit = 3
            setPageTransformer(false, NoPageTransformer())
        }
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
        }
    }

    private class ListTypeAdapter(context: Context) : ArrayAdapter<String>(
        context,
        R.layout.list_type_adapter_view, R.id.tv_type
    ) {
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
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.list_type_adapter_view,
                parent,
                false
            )
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

    companion object {
        private val DEFAULT_DISPLAY_MODE = LibraryViewModel.DisplayMode.PLAYLISTS.name
    }
}
