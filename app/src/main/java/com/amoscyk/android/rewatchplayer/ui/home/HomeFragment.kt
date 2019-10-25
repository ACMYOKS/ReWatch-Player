package com.amoscyk.android.rewatchplayer.ui.home


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.amoscyk.android.rewatchplayer.R

class HomeFragment : Fragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar

    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_home, container, false)
            bindViews()
            setupOptionMenu()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setupWithNavController(findNavController())
    }

    private fun bindViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
    }

    private fun setupOptionMenu() {
        toolbar.inflateMenu(R.menu.home_option_menu)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search_video -> {
                    findNavController().navigate(HomeFragmentDirections.gotoVideoSearchPage())
                }
            }
            true
        }
    }

}
