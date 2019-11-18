package com.amoscyk.android.rewatchplayer.ui.library


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.ui.CommonListDecoration
import com.amoscyk.android.rewatchplayer.ui.PlaylistListAdapter
import com.amoscyk.android.rewatchplayer.viewModelFactory

class LibraryFragment : Fragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private lateinit var loadPlaylistBtn: Button
    private lateinit var loadMoreBtn: Button
    private lateinit var libraryList: RecyclerView
    private val viewModel by viewModels<LibraryViewModel> { viewModelFactory }

    private val mPlaylistAdapter = PlaylistListAdapter(itemOnClick = {
        findNavController().navigate(LibraryFragmentDirections.showVideoList(it))
    })

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel.editMode.observe(this, Observer {
            showOptionMenu(it)
        })
        viewModel.currentDisplayMode.observe(this, Observer {
            setMenuItemForDisplayMode(it)
        })
        viewModel.playlistList.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    mPlaylistAdapter.submitList(resource.data)
                }
                Status.ERROR -> {
                    (resource.message as? Exception)?.let { exception ->
                        Log.d("LOG", exception.message ?: "has exception")
                    }
                }
                Status.LOADING -> {

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


    private fun setupViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
        libraryList = rootView!!.findViewById(R.id.library_list)
        loadPlaylistBtn = rootView!!.findViewById(R.id.load_playlist_btn)
        loadMoreBtn = rootView!!.findViewById(R.id.load_more_playlist_btn)

        toolbar.setupWithNavController(findNavController())
        libraryList.layoutManager = LinearLayoutManager(requireContext())
        libraryList.addItemDecoration(CommonListDecoration(8))
        libraryList.adapter = mPlaylistAdapter
        loadPlaylistBtn.setOnClickListener {
            viewModel.loadPlaylists()
        }
        loadMoreBtn.setOnClickListener {
            viewModel.loadMorePlaylists()
        }
    }

    private fun setupOptionMenu() {
        toolbar.inflateMenu(R.menu.library_option_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search_item -> {

                }
                R.id.show_all_items -> {
                    viewModel.setDisplayMode(LibraryViewModel.DisplayMode.SHOW_ALL)
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

    private fun showOptionMenu(isEditMode: Boolean) {
        with(toolbar.menu) {
            setGroupVisible(R.id.library_option_group, !isEditMode)
            setGroupVisible(R.id.library_edit_group, isEditMode)
        }
    }

    private fun setMenuItemForDisplayMode(displayMode: LibraryViewModel.DisplayMode) {
        with(toolbar.menu) {
            findItem(R.id.show_all_items).isVisible = displayMode == LibraryViewModel.DisplayMode.PLAYLISTS
            findItem(R.id.show_playlists).isVisible = displayMode == LibraryViewModel.DisplayMode.SHOW_ALL
        }
    }

}
