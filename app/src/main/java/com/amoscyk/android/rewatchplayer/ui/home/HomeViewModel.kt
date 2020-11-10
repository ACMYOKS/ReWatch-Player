package com.amoscyk.android.rewatchplayer.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.ui.RPViewModel

class HomeViewModel(application: Application, youtubeRepository: YoutubeRepository) :
    RPViewModel(application, youtubeRepository) {

}