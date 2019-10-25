package com.amoscyk.android.rewatchplayer.ui.library


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.amoscyk.android.rewatchplayer.R

class LibraryFragment : Fragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private val viewModel by viewModels<LibraryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            // Inflate the layout for this fragment
            rootView = inflater.inflate(R.layout.fragment_library, container, false)
            bindViews()
            setupOptionMenu()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setupWithNavController(findNavController())
        viewModel.editMode.observe(viewLifecycleOwner, Observer {
            showOptionMenu(it)
        })
        viewModel.currentDisplayMode.observe(viewLifecycleOwner, Observer {
            setMenuItemForDisplayMode(it)
        })
    }

    private fun bindViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
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
