package com.amoscyk.android.rewatchplayer

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.VideoListViewModel
import com.amoscyk.android.rewatchplayer.ui.account.StartupAccountViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadFileDetailViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadManagerFragment
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadManagerViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadPageViewModel
import com.amoscyk.android.rewatchplayer.ui.home.HomeViewModel
import com.amoscyk.android.rewatchplayer.ui.home.VideoSearchViewModel
import com.amoscyk.android.rewatchplayer.ui.library.LibraryViewModel
import com.amoscyk.android.rewatchplayer.ui.player.PlayerViewModel

class ViewModelFactory(
    private val youtubeRepository: YoutubeRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                return MainViewModel(youtubeRepository) as T
            }
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
                return LibraryViewModel(youtubeRepository) as T
            }
            modelClass.isAssignableFrom(VideoListViewModel::class.java) -> {
                return VideoListViewModel(youtubeRepository) as T
            }
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                return PlayerViewModel(youtubeRepository) as T
            }
            modelClass.isAssignableFrom(DownloadManagerViewModel::class.java) -> {
                return DownloadManagerViewModel(youtubeRepository) as T
            }
            modelClass.isAssignableFrom(DownloadFileDetailViewModel::class.java) -> {
                return DownloadFileDetailViewModel(youtubeRepository) as T
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