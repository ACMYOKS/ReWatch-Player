package com.amoscyk.android.rewatchplayer.ui.library

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LibraryViewModel: ViewModel() {
    enum class DisplayMode {
        PLAYLISTS,
        SHOW_ALL
    }

    private val _editMode = MutableLiveData<Boolean>()
    val editMode: LiveData<Boolean> = _editMode

    // set display mode from user preference
    private val _currentDisplayMode = MutableLiveData<DisplayMode>()
    val currentDisplayMode: LiveData<DisplayMode> = _currentDisplayMode

    init {
        _editMode.value = false
        _currentDisplayMode.value = DisplayMode.PLAYLISTS
    }

    fun setDisplayMode(displayMode: DisplayMode) {
        if (_currentDisplayMode.value == displayMode) return
        _currentDisplayMode.value = displayMode
    }

    fun setEditMode(isActive: Boolean) {
        if (_editMode.value == isActive) return
        _editMode.value = isActive
    }
}