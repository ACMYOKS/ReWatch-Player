package com.amoscyk.android.rewatchplayer.ui.home


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ui.player.PlayerActivity
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {

    private val toolbar get() = view!!.toolbar
    private val button get() = view!!.btn

    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setupWithNavController(findNavController())
        setupOptionMenu()
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
