package com.amoscyk.android.rewatchplayer.ui.setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import com.amoscyk.android.rewatchplayer.*
import com.amoscyk.android.rewatchplayer.util.*
import kotlinx.android.synthetic.main.fragment_settings.view.*

class SettingsFragment : ReWatchPlayerFragment() {
    private val viewModel by viewModels<SettingsViewModel> { viewModelFactory }

    private val toolbar get() = view!!.toolbar
    private val cellSkipForward get() = view!!.cell_skip_forward
    private val cellSkipBackward get() = view!!.cell_skip_backward
    private val cellEnablePip get() = view!!.cell_enable_pip
    private val cellAllowVideoStreamEnv get() = view!!.cell_allow_video_stream_env
    private val cellAllowPlayInBackground get() = view!!.cell_allow_play_when_screen_off
    private val cellPlayDownloadedQuality get() = view!!.cell_play_downloaded_quality
    private val cellAllowDownloadNetworkEnv get() = view!!.cell_allow_download_network_env

    // preference values
    private val skipTimeStrings by lazy {
        requireContext().resources.getStringArray(R.array.player_skip_time_entries)
    }
    private val skipTimeValues by lazy {
        requireContext().resources.getIntArray(R.array.player_skip_time_values)
    }

    private val dataUsageNetworkType by lazy {
        requireContext().resources.getStringArray(R.array.data_usage_network_type)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        setupNeedRefreshView()
    }

    private fun setupViews() {
        toolbar.apply {
            title = getString(R.string.settings_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { activity?.finish() }
        }
        cellSkipForward.apply {
            setTitle(getString(R.string.settings_player_skip_forward))
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(
                        skipTimeStrings,
                        skipTimeValues.indexOf(getSkipForwardPref())
                    ) { d, i ->
                        requireContext().appSharedPreference.edit {
                            putInt(PreferenceKey.PLAYER_SKIP_FORWARD_TIME, skipTimeValues[i])
                        }
                        setCurrentValue(skipTimeStrings[i])
                        d.dismiss()
                    }
                    .setNeutralButton(R.string.default_text) { d, i ->
                        requireContext().appSharedPreference.edit {
                            putInt(
                                PreferenceKey.PLAYER_SKIP_FORWARD_TIME,
                                AppSettings.DEFAULT_PLAYER_SKIP_FORWARD_SECOND
                            )
                        }
                        setCurrentValue(skipTimeStrings[skipTimeValues.indexOf(AppSettings.DEFAULT_PLAYER_SKIP_FORWARD_SECOND)])
                    }
                    .create()
                    .show()
            }
            setCurrentValue(skipTimeStrings[skipTimeValues.indexOf(getSkipForwardPref())])
        }
        cellSkipBackward.apply {
            setTitle(getString(R.string.settings_player_skip_backward))
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(
                        skipTimeStrings,
                        skipTimeValues.indexOf(getSkipBackwardPref())
                    ) { d, i ->
                        requireContext().appSharedPreference.edit {
                            putInt(PreferenceKey.PLAYER_SKIP_BACKWARD_TIME, skipTimeValues[i])
                        }
                        setCurrentValue(skipTimeStrings[i])
                        d.dismiss()
                    }
                    .setNeutralButton(R.string.default_text) { d, i ->
                        requireContext().appSharedPreference.edit {
                            putInt(
                                PreferenceKey.PLAYER_SKIP_BACKWARD_TIME,
                                AppSettings.DEFAULT_PLAYER_SKIP_BACKWARD_SECOND
                            )
                        }
                        setCurrentValue(skipTimeStrings[skipTimeValues.indexOf(AppSettings.DEFAULT_PLAYER_SKIP_BACKWARD_SECOND)])
                    }
                    .create()
                    .show()
            }
            setCurrentValue(skipTimeStrings[skipTimeValues.indexOf(getSkipBackwardPref())])
        }
        cellPlayDownloadedQuality.apply {
            setTitle(getString(R.string.settings_player_play_downloaded_quality))
            setTextOff(getString(R.string.settings_player_play_downloaded_quality_off))
            setTextOn(getString(R.string.settings_player_play_downloaded_quality_on))
            setOnClickListener {
                toggleSelection()
            }
            setCheckStateObserver { isChecked ->
                requireContext().appSharedPreference.edit {
                    putBoolean(PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST, isChecked)
                }
            }
            setChecked(
                requireContext().appSharedPreference.getBoolean(
                    PreferenceKey.PLAYER_PLAY_DOWNLOADED_IF_EXIST,
                    AppSettings.DEFAULT_PLAYER_PLAY_DOWNLOADED_IF_EXIST
                )
            )
        }
        cellAllowVideoStreamEnv.apply {
            setTitle(getString(R.string.settings_allow_video_streaming_env))
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(dataUsageNetworkType, getAllowVideoStreamingEnvPref()) { d, i ->
                        setCurrentValue(dataUsageNetworkType[i])
                        requireContext().appSharedPreference.edit {
                            putInt(PreferenceKey.ALLOW_VIDEO_STREAMING_ENV, i)
                        }
                        d.dismiss()
                    }
                    .create()
                    .show()
            }
            setCurrentValue(dataUsageNetworkType[getAllowVideoStreamingEnvPref()])
        }
        cellAllowPlayInBackground.apply {
            setTitle(getString(R.string.settings_allow_play_in_background))
            setDescription(getString(R.string.settings_allow_play_in_background_description))
            setOnClickListener { toggleSelection() }
            setCheckStateObserver { isChecked ->
                requireContext().appSharedPreference.edit {
                    putBoolean(PreferenceKey.ALLOW_PLAY_IN_BACKGROUND, isChecked)
                }
                setCurrentValueVisible(false)
            }
            setChecked(
                requireContext().appSharedPreference.getBoolean(
                    PreferenceKey.ALLOW_PLAY_IN_BACKGROUND,
                    AppSettings.DEFAULT_ALLOW_PLAY_IN_BACKGROUND
                )
            )
        }
        cellAllowDownloadNetworkEnv.apply {
            setTitle(getString(R.string.settings_allow_download_env))
            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setSingleChoiceItems(dataUsageNetworkType, getAllowDownloadEnvPref()) { d, i ->
                        setCurrentValue(dataUsageNetworkType[i])
                        requireContext().appSharedPreference.edit {
                            putInt(PreferenceKey.ALLOW_DOWNLOAD_ENV, i)
                        }
                        d.dismiss()
                    }
                    .create()
                    .show()
            }
            setCurrentValue(dataUsageNetworkType[getAllowDownloadEnvPref()])
        }
    }

    private fun setupNeedRefreshView() {
        cellEnablePip.apply {
            val pipSupported = AppSettings.isPipSupported(requireContext())
            val pipEnabled = AppSettings.isPipEnabled(requireContext())
            setTitle(getString(R.string.settings_player_enable_pip))
            setDescription(
                if (!pipSupported)
                    getString(R.string.settings_player_enable_pip_not_supported)
                else if (pipEnabled) {
                    getString(R.string.settings_player_enable_pip_description)
                } else {
                    getString(R.string.settings_player_enable_pip_disabled)
                }
            )
            setOnClickListener {
                if (pipSupported) {
                    if (pipEnabled) {
                        toggleSelection()
                    } else {
                        startActivityForResult(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}")),
                            REQUEST_CODE_OPEN_APP_SETTINGS)
                    }
                }
            }
            setCheckStateObserver { isChecked ->
                setDescriptionVisible(!isChecked)
                setCurrentValueVisible(isChecked)
                requireContext().appSharedPreference.edit {
                    putBoolean(PreferenceKey.PLAYER_ENABLE_PIP, isChecked)
                }
            }
            setChecked(
                requireContext().appSharedPreference.getBoolean(
                    PreferenceKey.PLAYER_ENABLE_PIP,
                    AppSettings.DEFAULT_PLAYER_ENABLE_PIP
                )
            )
            setSwitchVisibility(pipEnabled)
            isEnabled = pipSupported
        }
    }

    private fun getSkipForwardPref() = requireContext().appSharedPreference.getInt(
        PreferenceKey.PLAYER_SKIP_FORWARD_TIME,
        AppSettings.DEFAULT_PLAYER_SKIP_FORWARD_SECOND
    )

    private fun getSkipBackwardPref() = requireContext().appSharedPreference.getInt(
        PreferenceKey.PLAYER_SKIP_BACKWARD_TIME,
        AppSettings.DEFAULT_PLAYER_SKIP_BACKWARD_SECOND
    )

    private fun getAllowVideoStreamingEnvPref() = requireContext().appSharedPreference.getInt(
        PreferenceKey.ALLOW_VIDEO_STREAMING_ENV,
        AppSettings.DEFAULT_ALLOW_DOWNLOAD_ENV
    )

    private fun getAllowDownloadEnvPref() = requireContext().appSharedPreference.getInt(
        PreferenceKey.ALLOW_DOWNLOAD_ENV,
        AppSettings.DEFAULT_ALLOW_DOWNLOAD_ENV
    )

    companion object {
        private const val REQUEST_CODE_OPEN_APP_SETTINGS = 1000
    }
}