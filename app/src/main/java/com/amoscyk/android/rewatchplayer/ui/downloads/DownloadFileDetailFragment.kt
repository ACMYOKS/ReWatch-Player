package com.amoscyk.android.rewatchplayer.ui.downloads

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.datasource.vo.local.PlayerResource
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.util.DateTimeHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper
import com.amoscyk.android.rewatchplayer.util.formatReadableByteUnit
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.download_file_list_item.view.*
import kotlinx.android.synthetic.main.fragment_download_file_detail.view.*
import kotlinx.coroutines.launch

class DownloadFileDetailFragment: ReWatchPlayerFragment() {
    private val mToolbar get() = view!!.toolbar
    private val mRvFileStatus get() = view!!.rv_download_status
    private val mDetailContainer get() = view!!.detail_container
    private val mIvPreview get() = view!!.iv_preview
    private val mTvDuration get() = view!!.tv_duration
    private val mTvTitle get() = view!!.tv_title
    private val mTvAuthor get() = view!!.tv_author
    private val mTvVideoId get() = view!!.tv_video_id
    private lateinit var mDlMngr: DownloadManager
    private lateinit var mDlObserver: DownloadProgressObserver
    private val mDlHandler = Handler(Looper.getMainLooper())
    private var mAdapter: DownloadedFileAdapter? = null
    private val mDialogDelete by lazy {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure to remove the selected items?")
            .setPositiveButton("ok") { _, _ ->
                lifecycleScope.launch {
                    val result = viewModel.deleteSelectedPlayerResources(requireContext())
                    if (result.status == Status.SUCCESS) {
                        mainFragment?.showSnackbar("Item(s) deleted!", Snackbar.LENGTH_SHORT)
                        viewModel.getVideoMetaWithPlayerResource(listOf(navArgs.videoId))
                    } else {
                        mainFragment?.showSnackbar(result.message as? String ?: "error",
                            Snackbar.LENGTH_SHORT)
                    }
                    actionMode?.finish()
                }
            }
            .setNegativeButton("cancel") { _, _ -> }
            .create()
    }
    private var actionMode: ActionMode? = null
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.download_manager_action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.delete -> {
                    mDialogDelete.show()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (actionMode != null) actionMode = null
            viewModel.setEditMode(false)
        }
    }

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel: DownloadFileDetailViewModel by viewModels { viewModelFactory }
    private val navArgs: DownloadFileDetailFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mDlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        mDlObserver = DownloadProgressObserver(mDlHandler)
        mAdapter = DownloadedFileAdapter().apply {
            setHasStableIds(true)
        }
        viewModel.videoMetas.observe(this, Observer { metas ->
            metas.firstOrNull()?.let { res ->
                mIvPreview.load(YouTubeVideoThumbnailHelper.getStandardUrl(res.videoMeta.videoId))
                mTvDuration.text = DateTimeHelper.getDisplayString(res.videoMeta.duration)
                mTvTitle.text = res.videoMeta.title
                mTvAuthor.text = res.videoMeta.channelTitle
                mTvVideoId.text = res.videoMeta.videoId
                mDetailContainer.setOnClickListener {
                    if (viewModel.isEditMode.value == false) {
                        mainViewModel.readyVideo(res.videoMeta.videoId)
                    }
                }
                mAdapter?.submitList(res.playerResources.map { it.toExt() })
                viewModel.updateDownloadStatus(requireContext())
            }
        })
        viewModel.downloadStatus.observe(this, Observer { status ->
            mAdapter?.updateDownloadProgress(status)
        })
        viewModel.isEditMode.observe(this, Observer {
            mAdapter?.setEditMode(it)
            if (it) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    actionModeCallback
                )
            } else {
                actionMode?.finish()
            }
        })
        viewModel.totalSize.observe(this, Observer {
//            mToolbar.title = it.formatReadableByteUnit()
        })
        object : MediatorLiveData<String>() {
            var selectedSize = 0L
            var count = 0
            init {
                addSource(viewModel.selectedSize) {
                    selectedSize = it
                    emitString()
                }
                addSource(viewModel.selectedCount) {
                    count = it
                    emitString()
                }
            }
            fun emitString() {
                value = "$count (${selectedSize.formatReadableByteUnit()})"
            }
        }.observe(this, Observer {
            actionMode?.title = it
        })
        mainViewModel.archiveResult.observe(this, Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    viewModel.getVideoMetaWithPlayerResource(listOf(it.data!!.videoId))
                }
                Status.ERROR -> {

                }
                else -> {}
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_download_file_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().apply {
            contentResolver.registerContentObserver(CONTENT_URI, true, mDlObserver)
            viewModel.getVideoMetaWithPlayerResource(listOf(navArgs.videoId))
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().apply {
            contentResolver.unregisterContentObserver(mDlObserver)
        }
    }


    private fun setupView() {
        mToolbar.apply {
            setupWithNavController(findNavController())
        }
        mRvFileStatus.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
    }

    private fun PlayerResource.toExt(): PlayerResourceExt {
        return PlayerResourceExt(
            videoId = videoId,
            downloadId = downloadId,
            itag = itag,
            totalFileSize = fileSize.toInt(),
            currentFileSize = 0,
            status = -1,
            reason = -1
        )
    }

    inner class DownloadProgressObserver(handler: Handler): ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "progress updated")
            viewModel.updateDownloadStatus(requireContext())
        }
    }

    private inner class DownloadedFileAdapter :
        ListAdapter<PlayerResourceExt, DownloadedFileAdapter.ViewHolder>(DIFF_CALLBACK) {
        private val checkStatus = hashMapOf<Long, Boolean>()
        private var isEditMode = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.download_file_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            if (checkStatus[item.downloadId] == null) checkStatus[item.downloadId] = false
            holder.apply {
                tvDescription.text = YouTubeStreamFormatCode.FORMAT_CODES[item.itag]?.
                    let { it.resolution ?: it.bitrate }
                selectBox.visibility = if (isEditMode) View.VISIBLE else View.GONE
                selectBox.isChecked = checkStatus[item.downloadId]!!
                setViewForExtraData(item.totalFileSize, item.currentFileSize, item.status, item.reason)
            }
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).downloadId
        }

        fun setEditMode(isOn: Boolean) {
            isEditMode = isOn
            if (!isOn) {
                checkStatus.keys.forEach { checkStatus[it] = false }
            }
            notifyDataSetChanged()
        }

        fun updateDownloadProgress(status: Map<Long, DownloadStatus>) {
            for (i in 0 until itemCount) {
                val item = getItem(i)
                status[item.downloadId]?.let {
                    item.currentFileSize = it.downloadedByte
                    item.totalFileSize = it.totalByte
                    item.status = it.downloadStatus
                    item.reason = it.statusReason
                    notifyItemChanged(i)
                }
            }
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val tvDescription: TextView = itemView.tv_description
            val tvFileSize: TextView = itemView.tv_file_size
            val tvCompletedFileSize: TextView = itemView.tv_completed_file_size
            val tvStatusReason: TextView = itemView.tv_status_reason
            val pbDownload: ProgressBar = itemView.pb_download
            val selectBox: CheckBox = itemView.checkbox_select

            init {
                setViewForDownloadCompleted(false)
                itemView.setOnClickListener {
                    if (viewModel.isEditMode.value == true) {
                        selectBox.isChecked = !selectBox.isChecked
                    }
                }
                itemView.setOnLongClickListener {
                    if (viewModel.isEditMode.value == true) {
                        selectBox.isChecked = !selectBox.isChecked
                    } else {
                        selectBox.isChecked = true
                    }
                    true
                }
                selectBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    val item = getItem(adapterPosition)
                    checkStatus[item.downloadId] = isChecked
                    if (isChecked) viewModel.selectDownloadedItem(listOf(item.downloadId))
                    else viewModel.deselectDownloadedItem(listOf(item.downloadId))
                }
            }

            private fun setViewForDownloadCompleted(isCompleted: Boolean) {
                if (isCompleted) {
                    tvFileSize.visibility = View.GONE
                    tvCompletedFileSize.visibility = View.VISIBLE
                    pbDownload.visibility = View.GONE
                } else {
                    tvFileSize.visibility = View.VISIBLE
                    tvCompletedFileSize.visibility = View.GONE
                    pbDownload.visibility = View.VISIBLE
                }
            }

            @SuppressLint("SetTextI18n")
            fun setViewForExtraData(totalFileSize: Int, currentFileSize: Int, status: Int, reason: Int) {
                if (currentFileSize < totalFileSize) {
                    setViewForDownloadCompleted(false)
                    tvFileSize.text =
                        "${currentFileSize.toLong().formatReadableByteUnit()}/${totalFileSize.toLong().formatReadableByteUnit()}"
                    pbDownload.apply {
                        max = totalFileSize
                        progress = currentFileSize
                    }
                } else {
                    setViewForDownloadCompleted(true)
                    tvCompletedFileSize.text = totalFileSize.toLong().formatReadableByteUnit()
                }
                if (status >= 0 && reason >= 0) {
                    when (status) {
                        DownloadManager.STATUS_PENDING -> {
                            tvCompletedFileSize.visibility = View.GONE
                            tvFileSize.visibility = View.GONE
                            pbDownload.visibility = View.GONE
                            tvStatusReason.visibility = View.VISIBLE
                            tvStatusReason.text = getString(R.string.download_status_pending)
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            tvCompletedFileSize.visibility = View.GONE
                            tvFileSize.visibility = View.GONE
                            pbDownload.visibility = View.GONE
                            tvStatusReason.visibility = View.VISIBLE
                            tvStatusReason.text = getString(R.string.download_status_paused) + ": " +
                                    getString(
                                        when (reason) {
                                            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> R.string.download_reason_paused_queued_for_wifi
                                            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> R.string.download_reason_paused_waiting_for_network
                                            DownloadManager.PAUSED_WAITING_TO_RETRY -> R.string.download_reason_paused_waiting_to_retry
                                            else -> R.string.download_reason_paused_unknown
                                        }
                                    )
                        }
                        DownloadManager.STATUS_FAILED -> {
                            tvCompletedFileSize.visibility = View.GONE
                            tvFileSize.visibility = View.GONE
                            pbDownload.visibility = View.GONE
                            tvStatusReason.visibility = View.VISIBLE
                            tvStatusReason.text = getString(R.string.download_status_failed) + ": " +
                                    getString(
                                        when (reason) {
                                            DownloadManager.ERROR_CANNOT_RESUME -> R.string.download_reason_error_cannot_resume
                                            DownloadManager.ERROR_DEVICE_NOT_FOUND -> R.string.download_reason_error_device_not_found
                                            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> R.string.download_reason_error_file_already_exists
                                            DownloadManager.ERROR_FILE_ERROR -> R.string.download_reason_error_file_error
                                            DownloadManager.ERROR_HTTP_DATA_ERROR -> R.string.download_reason_error_http_data_error
                                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> R.string.download_reason_error_insufficient_space
                                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> R.string.download_reason_error_too_many_redirect
                                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> R.string.download_reason_error_unhandled_http_code
                                            else -> R.string.player_error_unknown
                                        }
                                    )
                        }
                        else -> { tvStatusReason.visibility = View.GONE }
                    }
                } else {
                    tvStatusReason.visibility = View.GONE
            }
            }

        }
    }

    private data class PlayerResourceExt(
        var videoId: String,
        var downloadId: Long,
        var itag: Int,
        var totalFileSize: Int,
        var currentFileSize: Int,
        var status: Int,
        var reason: Int
    )

    companion object {
        const val TAG = "DownloadFragment"
        private val CONTENT_URI = Uri.parse("content://downloads/my_downloads")
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlayerResourceExt>() {
            override fun areItemsTheSame(oldItem: PlayerResourceExt, newItem: PlayerResourceExt): Boolean {
                return oldItem.videoId == newItem.videoId &&
                        oldItem.itag == newItem.itag &&
                        oldItem.downloadId == newItem.downloadId &&
                        oldItem.currentFileSize == newItem.currentFileSize &&
                        oldItem.totalFileSize == newItem.totalFileSize
            }

            override fun areContentsTheSame(oldItem: PlayerResourceExt, newItem: PlayerResourceExt): Boolean {
                return oldItem == newItem
            }
        }
    }

}