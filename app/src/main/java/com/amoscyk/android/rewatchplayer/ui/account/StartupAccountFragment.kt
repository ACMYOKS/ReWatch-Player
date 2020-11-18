package com.amoscyk.android.rewatchplayer.ui.account

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.amoscyk.android.rewatchplayer.youtubeServiceProvider
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class StartupAccountFragment : ReWatchPlayerFragment(), EasyPermissions.PermissionCallbacks {

    private var rootView: View? = null
    private val viewModel by viewModels<StartupAccountViewModel> { viewModelFactory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
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
                    findNavController().navigate(StartupAccountFragmentDirections.showMainPage())
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_startup_account, container, false)
            setupViews()
        }
        return rootView
    }

    private fun setupViews() {
        val tv1 = rootView!!.findViewById<TextView>(R.id.tv_msg1)
        val tv2 = rootView!!.findViewById<TextView>(R.id.tv_msg2)
        val signupBtn = rootView!!.findViewById<Button>(R.id.signup_btn)
        signupBtn!!.setOnClickListener {
            chooseUserAccount()
        }
        tv1.fadeInAndSlide(false, 0)
        tv2.fadeInAndSlide(false, 1000)
        signupBtn.fadeInAndSlide(true, 2200)
    }

    private fun View.fadeInAndSlide(up: Boolean, delay: Long) {
        this.translationY = if (up) 100f else -100f
        this.alpha = 0f
        animate()
            .setStartDelay(delay)
            .alpha(1f)
            .translationY(0f)
            .start()
    }

    @AfterPermissionGranted(REQUEST_GET_ACCOUNT_PERMISSION)
    private fun chooseUserAccount() {
        viewModel.chooseUserAccount(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ACCOUNT_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.let { accountName ->
                        viewModel.setUserAccountName(requireContext(), accountName)
                    }
                }
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
        private const val REQUEST_GET_ACCOUNT_PERMISSION = 1000
        private const val REQUEST_ACCOUNT_PICKER = 1001
    }

}
