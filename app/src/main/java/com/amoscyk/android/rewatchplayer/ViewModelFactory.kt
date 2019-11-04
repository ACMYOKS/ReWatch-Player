package com.amoscyk.android.rewatchplayer

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.ui.account.StartupAccountViewModel
import com.amoscyk.android.rewatchplayer.ui.home.HomeViewModel
import com.amoscyk.android.rewatchplayer.ui.home.VideoSearchViewModel
import com.amoscyk.android.rewatchplayer.ui.library.LibraryViewModel

class ViewModelFactory(
    private val youtubeRepository: YoutubeRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(StartupAccountViewModel::class.java) -> {
                return StartupAccountViewModel() as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                return HomeViewModel() as T
            }
            modelClass.isAssignableFrom(VideoSearchViewModel::class.java) -> {
                return VideoSearchViewModel(youtubeRepository) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                return LibraryViewModel() as T
            }
            else -> {
                return super.create(modelClass)
            }
        }
    }

}

val Activity.viewModelFactory: ViewModelFactory
    get() {
        val youtubeRepository = (this.application as ReWatchPlayerApplication).youtubeRepository
        return ViewModelFactory(youtubeRepository)
    }

val Fragment.viewModelFactory: ViewModelFactory
    get() {

        return requireActivity().viewModelFactory
    }