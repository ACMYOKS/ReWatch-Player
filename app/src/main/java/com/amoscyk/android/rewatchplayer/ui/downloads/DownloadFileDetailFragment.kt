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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
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
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadPageViewModel.MenuState
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.download_file_list_item.view.*
import kotlinx.android.synthetic.main.fragment_download_file_detail.view.*
import kotlinx.coroutines.launch

class DownloadFileDetailFragment: ReWatchPlayerFragment() {

    private var mRootView: View? = null
    private val mToolbar by lazy { mRootView!!.toolbar }
    private val mRvFileStatus by lazy { mRootView!!.rv_download_status }
    private val mDetailContainer by lazy { mRootView!!.detail_container }
    private val mIvPreview by lazy { mRootView!!.iv_preview }
    private val mTvTitle by lazy { mRootView!!.tv_title }
    private val mTvAuthor by lazy { mRootView!!.tv_author }
    private val mTvVideoId by lazy { mRootView!!.tv_video_id }
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
                        Snackbar.make(view!!, "Item(s) deleted!", Snackbar.LENGTH_SHORT).show()
                        viewModel.getVideoMetaWithPlayerResource(listOf(navArgs.videoId))
                    } else {
                        Snackbar.make(view!!, (result.message as? String) ?: "error",
                            Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("cancel") { _, _ -> }
            .create()
    }

    private val mainViewModel by activityViewModels<MainViewModel> { viewModelFactory }
    private val viewModel: DownloadFileDetailViewModel by viewModels { viewModelFactory }
    private val navArgs: DownloadFileDetailFragmentArgs by navArgs()

    private val mOnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            viewModel.setEditMode(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, mOnBackPressedCallback)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mDlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        mDlObserver = DownloadProgressObserver(mDlHandler)
        mAdapter = DownloadedFileAdapter()
        viewModel.videoMetas.observe(this, Observer { metas ->
            metas.firstOrNull()?.let { res ->
                mIvPreview.load(YouTubeVideoThumbnailHelper.getStandardUrl(res.videoMeta.videoId))
                mTvTitle.text = res.videoMeta.title
                mTvAuthor.text = res.videoMeta.channelTitle
                mTvVideoId.text = res.videoMeta.videoId
                mDetailContainer.setOnClickListener {
                    mainActivity?.playVideoForId(res.videoMeta.videoId)
                }
                mAdapter?.submitList(res.playerResources.map { it.toExt() })
                viewModel.updateDownloadStatus(requireContext())
            }
        })
        viewModel.downloadStatus.observe(this, Observer { status ->
            mAdapter?.updateDownloadProgress(status)
        })
        viewModel.menuState.observe(this, Observer {
            it?.let { setMenuItemForState(it) }
        })
        viewModel.isEditMode.observe(this, Observer {
            mOnBackPressedCallback.isEnabled = it
            mAdapter?.setEditMode(it)
        })
        object : MediatorLiveData<String>() {
            var totalSize = 0
            var selectedSize = 0
            var isEditMode = false
            init {
                addSource(viewModel.totalSize) {
                    totalSize = it
                    emitString()
                }
                addSource(viewModel.selectedSize) {
                    selectedSize = it
                    emitString()
                }
                addSource(viewModel.isEditMode) {
                    isEditMode = it
                    emitString()
                }
            }
            fun emitString() {
                this.value = if (isEditMode) {
                    selectedSize.toStringInMB() + "/" + totalSize.toStringInMB()
                } else {
                    totalSize.toStringInMB()
                }
            }
        }.observe(this, Observer {
            mToolbar.title = it
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
        if (mRootView == null) {
            mRootView = inflater.inflate(R.layout.fragment_download_file_detail, container, false)
            setupView()
        }
        return mRootView
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
            inflateMenu(R.menu.downloaded_file_option_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        mDialogDelete.show()
                    }
                }
                true
            }
        }
        mRvFileStatus.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
    }

    private fun setMenuItemForState(state: MenuState) {
        with (mToolbar.menu) {
            val delete = findItem(R.id.delete)
            when (state) {
                MenuState.NORMAL -> {
                    delete.isVisible = false
                }
                MenuState.SELECT_SINGLE, MenuState.SELECT_MULTI -> {
                    delete.isVisible = true
                }
            }
        }
    }

    private fun Int.toStringInMB(): String = "%.2fMB".format(this / 1_000_000f)
    private fun PlayerResource.toExt(): PlayerResourceExt {
        return PlayerResourceExt(
            videoId = videoId,
            downloadId = downloadId,
            itag = itag,
            totalFileSize = fileSize.toInt(),
            currentFileSize = 0
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
                setViewForFileSize(item.totalFileSize, item.currentFileSize)
            }
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
                    notifyItemChanged(i)
                }
            }
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val tvDescription: TextView = itemView.tv_description
            val tvFileSize: TextView = itemView.tv_file_size
            val tvCompletedFileSize: TextView = itemView.tv_completed_file_size
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
            fun setViewForFileSize(totalFileSize: Int, currentFileSize: Int) {
                if (currentFileSize < totalFileSize) {
                    setViewForDownloadCompleted(false)
                    tvFileSize.text = "${currentFileSize.toStringInMB()}/${totalFileSize.toStringInMB()}"
                    pbDownload.apply {
                        max = totalFileSize
                        progress = currentFileSize
                    }
                } else {
                    setViewForDownloadCompleted(true)
                    tvCompletedFileSize.text = totalFileSize.toStringInMB()
                }
            }

        }
    }

    private data class PlayerResourceExt(
        var videoId: String,
        var downloadId: Long,
        var itag: Int,
        var totalFileSize: Int,
        var currentFileSize: Int
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