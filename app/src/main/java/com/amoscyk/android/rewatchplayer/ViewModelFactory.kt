package com.amoscyk.android.rewatchplayer

import android.app.Activity
import android.app.Application
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.ui.ChannelViewModel
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.VideoListViewModel
import com.amoscyk.android.rewatchplayer.ui.setting.SettingsViewModel
import com.amoscyk.android.rewatchplayer.ui.account.StartupAccountViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadFileDetailViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadManagerViewModel
import com.amoscyk.android.rewatchplayer.ui.home.HomeViewModel
import com.amoscyk.android.rewatchplayer.ui.home.VideoSearchViewModel
import com.amoscyk.android.rewatchplayer.ui.library.LibraryViewModel
import com.amoscyk.android.rewatchplayer.ui.player.PlayerViewModel

class ViewModelFactory(
    private val application: Application,
    private val youtubeRepository: YoutubeRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                return MainViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(StartupAccountViewModel::class.java) -> {
                return StartupAccountViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                return HomeViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(VideoSearchViewModel::class.java) -> {
                return VideoSearchViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                return LibraryViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(ChannelViewModel::class.java) -> {
                return ChannelViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(VideoListViewModel::class.java) -> {
                return VideoListViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                return PlayerViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(DownloadManagerViewModel::class.java) -> {
                return DownloadManagerViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(DownloadFileDetailViewModel::class.java) -> {
                return DownloadFileDetailViewModel(application, youtubeRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                return SettingsViewModel(application, youtubeRepository) as T
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
        return ViewModelFactory(application, youtubeRepository)
    }

val Fragment.viewModelFactory: ViewModelFactory
    get() {

        return requireActivity().viewModelFactory
    }