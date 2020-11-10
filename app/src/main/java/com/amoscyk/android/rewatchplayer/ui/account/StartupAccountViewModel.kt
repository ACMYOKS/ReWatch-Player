package com.amoscyk.android.rewatchplayer.ui.account

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amoscyk.android.rewatchplayer.datasource.YoutubeRepository
import com.amoscyk.android.rewatchplayer.ui.RPViewModel
import com.amoscyk.android.rewatchplayer.util.PreferenceKey
import com.amoscyk.android.rewatchplayer.util.appSharedPreference
import com.amoscyk.android.rewatchplayer.util.putString
import pub.devrel.easypermissions.EasyPermissions

class StartupAccountViewModel(application: Application, youtubeRepository: YoutubeRepository) :
    RPViewModel(application, youtubeRepository) {
    enum class SettingStage {
        REQUEST_GET_ACCOUNT_PERMISSION,
        REQUEST_USER_ACCOUNT,
        USER_ACCOUNT_SELECTED
    }

    data class SettingStageMessenger(val stage: SettingStage, val username: String?)

    private val _settingStageMessenger = MutableLiveData<SettingStageMessenger>()
    val settingStageMessenger: LiveData<SettingStageMessenger> = _settingStageMessenger

    private val _accountName = MutableLiveData<String>()
    val accountName: LiveData<String> = _accountName

    fun chooseUserAccount(context: Context) {
        if (EasyPermissions.hasPermissions(context, Manifest.permission.GET_ACCOUNTS)) {
            _settingStageMessenger.value =
                SettingStageMessenger(SettingStage.REQUEST_USER_ACCOUNT, _accountName.value)
        } else {
            _settingStageMessenger.value =
                SettingStageMessenger(
                    SettingStage.REQUEST_GET_ACCOUNT_PERMISSION,
                    _accountName.value
                )
        }
    }

    fun setUserAccountName(context: Context, accountName: String) {
        context.appSharedPreference.edit {
            putString(PreferenceKey.ACCOUNT_NAME, accountName)
        }
        youtubeRepository.setAccountName(accountName)
        _accountName.value = accountName
        _settingStageMessenger.value =
            SettingStageMessenger(SettingStage.USER_ACCOUNT_SELECTED, _accountName.value)
    }
}
