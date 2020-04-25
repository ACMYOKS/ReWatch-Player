package com.amoscyk.android.rewatchplayer.ui.downloads

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper

class DownloadManagerViewModel(
    private val youtubeRepository: YoutubeRepository
): DownloadPageViewModel(youtubeRepository) {

    data class ViewStatus(
        var isSelected: Boolean
    )

    private var viewStatus = hashMapOf<String, ViewStatus>()
    private val videoMetaObserver = Observer<List<VideoMetaWithPlayerResource>> {
        it.forEach {
            if (viewStatus[it.videoMeta.videoId] == null)
                viewStatus[it.videoMeta.videoId] = ViewStatus(false)
        }
    }
    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode
    private val _menuState = MutableLiveData(MenuState.NORMAL)
    val menuState: LiveData<MenuState> = _menuState

    init {
        _videoMeta.observeForever(videoMetaObserver)
    }

    override fun onCleared() {
        _videoMeta.removeObserver(videoMetaObserver)
    }

    fun setEditMode(isOn: Boolean) {
        if (_isEditMode.value != isOn) {
            _isEditMode.value = isOn
            if (!isOn) {
                viewStatus.keys.forEach { deselectVideoMeta(it) }
            }
        }
    }

    fun selectVideoMeta(videoId: String) {
        setEditMode(true)
        viewStatus[videoId]?.isSelected = true
        handleMenuState()
    }

    fun deselectVideoMeta(videoId: String) {
        viewStatus[videoId]?.isSelected = false
        handleMenuState()
    }

    fun getSelectedVideoMetas(): List<VideoMetaWithPlayerResource> {
        val ids = viewStatus.filterValues { it.isSelected }.keys
        return _videoMeta.value.orEmpty().filter { ids.contains(it.videoMeta.videoId) }
    }

    suspend fun deleteSelectedPlayerResource(context: Context): Resource<Unit> {
        val ids = viewStatus.filterValues { it.isSelected }.keys.toTypedArray()
        val res = youtubeRepository.getPlayerResource(ids)
        val dbCount = youtubeRepository.deletePlayerResource(ids)
        if (dbCount < res.size) {
            return Resource.error(
                "some records cannot be deleted, expected: ${res.size}, actual: $dbCount",
                null)
        }
        val fileCount = res.fold(0) { acc, r ->
            val file = FileDownloadHelper.getFileByName(context, r.filename)
            if (file.exists() && file.delete()) {
                Log.d(AppConstant.TAG, "deleted file ${file.absolutePath}")
                acc + 1
            } else {
                Log.d(AppConstant.TAG, "cannot delete file ${file.absolutePath}")
                acc
            }
        }
        if (fileCount < res.size) {
            return Resource.error(
                "some file(s) cannot be deleted, expected: ${res.size}, actual: $fileCount",
                null)
        }
        return Resource.success(null)
    }

    private fun handleMenuState() {
        _menuState.value = when (getSelectedCount()) {
            0 -> MenuState.NORMAL
            1 -> MenuState.SELECT_SINGLE
            else -> MenuState.SELECT_MULTI
        }
    }

    private fun getSelectedCount() = viewStatus.values.count { it.isSelected }
}