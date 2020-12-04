package com.amoscyk.android.rewatchplayer.ui.account

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.amoscyk.android.rewatchplayer.*
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.WebViewFragmentDirections
import com.amoscyk.android.rewatchplayer.ui.setting.SettingsActivity
import com.amoscyk.android.rewatchplayer.util.getColorFromAttr
import com.amoscyk.android.rewatchplayer.util.setMenuItemTintColor
import kotlinx.android.synthetic.main.fragment_app_info.view.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class AppInfoFragment : ReWatchPlayerFragment(), EasyPermissions.PermissionCallbacks {

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel by viewModels<StartupAccountViewModel> { viewModelFactory }
    private val toolbar get() = view!!.toolbar
    private val currentUserLayout get() = view!!.current_user_layout
    private val tvCurrentAccount get() = view!!.tv_current_account
    private val cellTutorial get() = view!!.btn_tutorial
    private val cellAppInfo get() = view!!.btn_app_info
    private val cellCheckUpdate get() = view!!.btn_check_update
    private val cellFaq get() = view!!.btn_faq
    private val cellContact get() = view!!.btn_contact
    private val tvAppInfo get() = view!!.tv_app_info

    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModel.accountName.observe(this, Observer {
            tvCurrentAccount.text = it
        })
        viewModel.settingStageMessenger.observe(this, Observer {
            when (it.stage) {
                StartupAccountViewModel.SettingStage.REQUEST_GET_ACCOUNT_PERMISSION -> {
                    EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.settings_request_get_account_permission),
                        REQUEST_GET_ACCOUNT_PERMISSION,
                        Manifest.permission.GET_ACCOUNTS
                    )
                }
                StartupAccountViewModel.SettingStage.REQUEST_USER_ACCOUNT -> {
                    startActivityForResult(
                        youtubeServiceProvider.credential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER
                    )
                }
                StartupAccountViewModel.SettingStage.USER_ACCOUNT_SELECTED -> {

                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_app_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.apply {
            inflateMenu(R.menu.app_info_menu)
            setMenuItemTintColor(requireContext().getColorFromAttr(R.attr.colorOnPrimary))
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                    }
                }
                true
            }
        }
        currentUserLayout.apply {
            setOnClickListener {
                chooseAccount()
            }
        }
        tvCurrentAccount.text = youtubeServiceProvider.credential.selectedAccountName
        cellTutorial.apply {
            setTitle(getString(R.string.account_guide_title))
            setOnClickListener {
                findNavController().navigate(
                    WebViewFragmentDirections.showWebView(
                        AppConstant.APP_WEBSITE_URL + "guide?no_nav=true"
                    )
                )
            }
        }
        cellAppInfo.apply {
            setTitle(getString(R.string.account_app_info_title))
        }
        cellCheckUpdate.apply {
            setTitle(getString(R.string.account_check_update))
            setOnClickListener {
                mainViewModel.checkUpdate()
            }
        }
        cellFaq.apply {
            setTitle(getString(R.string.account_faq_title))
            setOnClickListener {
                findNavController().navigate(
                    WebViewFragmentDirections.showWebView(
                        AppConstant.APP_WEBSITE_URL + "faq?no_nav=true"
                    )
                )
            }
        }
        cellContact.apply {
            setTitle(getString(R.string.account_contact_title))
        }
        tvAppInfo.text = getString(R.string.app_info, getString(R.string.app_name), BuildConfig.VERSION_NAME)
    }

    @AfterPermissionGranted(REQUEST_GET_ACCOUNT_PERMISSION)
    private fun chooseAccount() {
        viewModel.chooseUserAccount(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.let { accountName ->
                viewModel.setUserAccountName(requireContext(), accountName)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_GET_ACCOUNT_PERMISSION) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                AppSettingsDialog.Builder(this).build().show()
            }
        }
    }

    companion object {
        private const val REQUEST_GET_ACCOUNT_PERMISSION = 1001
        private const val REQUEST_ACCOUNT_PICKER = 1002
    }

}