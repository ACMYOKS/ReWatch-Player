package com.amoscyk.android.rewatchplayer.ui.account


import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.util.*
import com.amoscyk.android.rewatchplayer.youtubeServiceProvider
import kotlinx.android.synthetic.main.fragment_settings.view.*
import kotlinx.android.synthetic.main.settings_header_view.view.*
import kotlinx.android.synthetic.main.settings_item_view.view.*

class SettingsFragment : ReWatchPlayerFragment() {

    private var mRootView: View? = null
    private val mRvSettings: RecyclerView by lazy { mRootView!!.rv_setting_item }

    private var mAdapter: SettingListAdapter? = null
    private lateinit var mSettingList: SettingList

    // plain var for option
    private var playOnlyUsingWifi = false
    private var username = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SPBooleanLiveData(context.appSharedPreference,
            PreferenceKey.PLAYER_ONLY_PLAY_WHEN_USING_WIFI, false).apply {
            observe(this@SettingsFragment, Observer {
                playOnlyUsingWifi = it
                mSettingList.getWithPosition(R.string.setting_player_play_only_using_wifi)?.apply {
                    (entry as? ListItemSwitch)?.value = playOnlyUsingWifi
                    mAdapter?.notifyItemChanged(position)
                }
            })
        }
        SPStringLiveData(context.appSharedPreference,
            PreferenceKey.ACCOUNT_NAME, "").apply {
            observe(this@SettingsFragment, Observer {
                username = it.orEmpty()
                mSettingList.getWithPosition(R.string.setting_account_account_name)?.apply {
                    (entry as? ListItemText)?.value = username
                    mAdapter?.notifyItemChanged(position)
                }
            })
        }
        mAdapter = SettingListAdapter().apply {
            setHasStableIds(true)
        }
        mSettingList = SettingList(
            ListHeader(R.string.setting_account),
            ListItemText(R.string.setting_account_account_name,
                username,
                onClickListener = {
                    startActivityForResult(youtubeServiceProvider.credential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_NAME)
                }),
            ListHeader(R.string.setting_player),
            ListItemSwitch(R.string.setting_player_play_only_using_wifi,
                playOnlyUsingWifi,
                onCheckChanged = {
                    requireContext().appSharedPreference.edit {
                        putBoolean(PreferenceKey.PLAYER_ONLY_PLAY_WHEN_USING_WIFI, it)
                    }
                    true
                },
                onClickListener = {
                    requireContext().appSharedPreference.edit {
                        putBoolean(PreferenceKey.PLAYER_ONLY_PLAY_WHEN_USING_WIFI, !playOnlyUsingWifi)
                    }
                }),
            ListHeader(R.string.setting_download),
            ListItemDetail(R.string.setting_download_download_location, "", onClickListener = {
                Toast.makeText(requireContext(), "Go to detail download page", Toast.LENGTH_SHORT).show()
            }),
            ListItemText(R.string.setting_download_storage_used, 0.toString())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_settings, container, false)
            setupViews()
        }
        return mRootView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // TODO: add permission checking
        if (requestCode == REQUEST_ACCOUNT_NAME && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.let { accountName ->
                youtubeServiceProvider.credential.selectedAccountName = accountName
                requireContext().appSharedPreference.edit {
                    putString(PreferenceKey.ACCOUNT_NAME, accountName)
                }
            }
        }
    }

    private fun setupViews() {
        mRvSettings.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
    }

    private enum class ViewType { HEADER, ITEM, ITEM_SWITCH, ITEM_TEXT }

    private inner class SettingListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return mSettingList.size
        }

        override fun getItemViewType(position: Int): Int {
            return mSettingList.getAt(position)!!.viewType.ordinal
        }

        override fun getItemId(position: Int): Long {
            return mSettingList.getAt(position)!!.titleId.toLong()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == ViewType.HEADER.ordinal) {
                Header(layoutInflater.inflate(R.layout.settings_header_view, parent, false))
            } else {
                Item(layoutInflater.inflate(R.layout.settings_item_view, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = mSettingList.getAt(position)!!
            val viewType = ViewType.values()[holder.itemViewType]
            if (viewType == ViewType.HEADER) {
                (holder as Header).apply {
                    tvHeader.text = getString(item.titleId)
                }
            } else {
                (holder as Item).apply {
                    tvItemName.text = getString(item.titleId)
                    holder.itemView.setOnClickListener { item.onClickListener?.invoke() }
                    when (viewType) {
                        ViewType.ITEM -> {
                            tvValue.visibility = View.INVISIBLE
                            switch.visibility = View.INVISIBLE
                            (item as ListItemDetail)
                        }
                        ViewType.ITEM_SWITCH -> {
                            tvValue.visibility = View.INVISIBLE
                            switch.visibility = View.VISIBLE
                            (item as ListItemSwitch).let {
                                switch.isChecked = it.value
                                switch.setOnCheckedChangeListener { _, isChecked ->
                                    it.onCheckChanged?.invoke(isChecked)
                                }
                            }
                        }
                        ViewType.ITEM_TEXT -> {
                            tvValue.visibility = View.VISIBLE
                            switch.visibility = View.INVISIBLE
                            tvValue.text = item.value.toString()
                        }
                        else -> {}
                    }
                }
            }
        }

        private inner class Header(itemView: View): RecyclerView.ViewHolder(itemView) {
            val tvHeader = itemView.tv_header
        }

        private inner class Item(itemView: View): RecyclerView.ViewHolder(itemView) {
            val tvItemName = itemView.tv_item_name
            val tvValue = itemView.tv_value
            val switch = itemView.switch_check
        }
    }

    private abstract class IListItem<T>(
        val viewType: ViewType,
        @StringRes val titleId: Int,
        var value: T,
        var onClickListener: (() -> Unit)?
    )

    private class ListHeader(@StringRes titleId: Int) : IListItem<Any>(ViewType.HEADER, titleId, Any(), null)

    private class ListItemSwitch(
        @StringRes titleId: Int, value: Boolean,
        onClickListener: (() -> Unit)? = null,
        var onCheckChanged: ((isChecked: Boolean) -> Boolean)? = null
    ) : IListItem<Boolean>(ViewType.ITEM_SWITCH, titleId, value, onClickListener)

    private class ListItemText(
        @StringRes titleId: Int, value: String,
        onClickListener: (() -> Unit)? = null
    ) : IListItem<String>(ViewType.ITEM_TEXT, titleId, value, onClickListener)

    private class ListItemDetail(
        @StringRes titleId: Int, value: String,
        onClickListener: (() -> Unit)? = null
    ) : IListItem<String>(ViewType.ITEM, titleId, value, onClickListener)

    private class SettingList(vararg items: IListItem<out Any>) {
        data class Holder(val position: Int, val entry: IListItem<out Any>)
        private val map = items.associateBy { it.titleId }
        private val keys = items.map { it.titleId }
        val size = items.size
        fun getAt(position: Int): IListItem<out Any>? {
            if (position in keys.indices) {
                return map[keys[position]]
            }
            return null
        }
        fun getById(id: Int): IListItem<out Any>? = map[id]
        fun getPosition(id: Int): Int = keys.indexOf(id)
        fun getWithPosition(id: Int): Holder? {
            val pos = getPosition(id)
            if (pos >= 0) {
                return Holder(pos, getAt(pos)!!)
            }
            return null
        }
    }

    companion object {
        private const val REQUEST_ACCOUNT_NAME = 1000
    }

}
