package com.amoscyk.android.rewatchplayer.ui.downloads

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import com.amoscyk.android.rewatchplayer.datasource.vo.Resource
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper

class DownloadFileDetailViewModel(
    private val youtubeRepository: YoutubeRepository
): DownloadPageViewModel(youtubeRepository) {

    data class ViewStatus(
        var isSelected: Boolean
    )

    private var viewStatus = hashMapOf<Long, ViewStatus>()      // downloadId -> viewStatus
    private val downloadStatusObserver = Observer<Map<Long, DownloadStatus>> {
        var sum = 0
        it.forEach {
            if (viewStatus[it.key] == null) {
                viewStatus[it.key] = ViewStatus(false)
            }
            sum += it.value.totalByte
        }
        if (sum != _totalSize.value) {
            _totalSize.value = sum
        }
    }
    private val _menuState = MutableLiveData(MenuState.NORMAL)
    val menuState: LiveData<MenuState> = _menuState
    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode
    private val _totalSize = MutableLiveData(0)
    val totalSize: LiveData<Int> = _totalSize
    private val _selectedSize = MutableLiveData(0)
    val selectedSize: LiveData<Int> = _selectedSize

    init {
        _downloadStatus.observeForever(downloadStatusObserver)
    }

    override fun onCleared() {
        super.onCleared()
        _downloadStatus.removeObserver(downloadStatusObserver)
    }

    fun setEditMode(isOn: Boolean) {
        if (_isEditMode.value != isOn) {
            _isEditMode.value = isOn
            if (!isOn) {
                deselectDownloadedItem(viewStatus.keys.toList())
            }
        }
    }

    fun selectDownloadedItem(ids: List<Long>) {
        setEditMode(true)
        _selectedSize.value = _selectedSize.value!! + ids.sumBy {
            viewStatus[it]?.let { status ->
                if (!status.isSelected) {
                    viewStatus[it]?.isSelected = true
                    return@sumBy _downloadStatus.value?.getValue(it)?.totalByte ?: 0
                }
            }
            return@sumBy 0
        }
        handleMenuState()
    }

    fun deselectDownloadedItem(ids: List<Long>) {
        _selectedSize.value = _selectedSize.value!! - ids.sumBy {
            viewStatus[it]?.let { status ->
                if (status.isSelected) {
                    status.isSelected = false
                    return@sumBy _downloadStatus.value?.getValue(it)?.totalByte ?: 0
                }
            }
            return@sumBy 0
        }
        handleMenuState()
    }

    suspend fun deleteSelectedPlayerResources(context: Context): Resource<Unit> {
        _videoMetas.value?.firstOrNull()?.let { videoMeta ->
            val downloadIds = viewStatus.filterValues { it.isSelected }.keys.toLongArray()
            val res = videoMeta.playerResources.filter { downloadIds.contains(it.downloadId) }
            val dbCount = youtubeRepository.deletePlayerResource(downloadIds)
            if (dbCount < res.size) {
                return Resource.error(
                    "some record(s) cannot be deleted, expected: ${res.size}, actual: $dbCount",
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
            downloadIds.forEach { viewStatus.remove(it) }
            return Resource.success(null)
        }
        return Resource.error("no video meta", null)
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