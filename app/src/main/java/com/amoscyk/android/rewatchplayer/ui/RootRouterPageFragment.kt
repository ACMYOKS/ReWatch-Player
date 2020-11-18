package com.amoscyk.android.rewatchplayer.ui


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.getString
import com.amoscyk.android.rewatchplayer.util.remove
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.amoscyk.android.rewatchplayer.youtubeServiceProvider

class RootRouterPageFragment : ReWatchPlayerFragment() {

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_root_router_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigateToNextPage()
    }

    private fun getSelectedAccountName(): String? {
        return requireContext().appSharedPreference.getString(PreferenceKey.ACCOUNT_NAME, null)
    }

    private fun navigateToNextPage() {
        getSelectedAccountName()?.let {
            mainViewModel.setAccountName(it)
            findNavController().navigate(RootRouterPageFragmentDirections.showMainPage())
        } ?: findNavController().navigate(RootRouterPageFragmentDirections.showStartupAccount())
    }

}
