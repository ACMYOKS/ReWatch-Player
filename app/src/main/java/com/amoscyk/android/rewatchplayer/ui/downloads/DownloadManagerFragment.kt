package com.amoscyk.android.rewatchplayer.ui.downloads


import android.app.DownloadManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import com.amoscyk.android.rewatchplayer.datasource.vo.local.DownloadedResource
import com.amoscyk.android.rewatchplayer.util.FileDownloadHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.viewModelFactory

class DownloadManagerFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private lateinit var rvDownloadStatus: RecyclerView
    private lateinit var mDlMngr: DownloadManager
    private lateinit var mDlObserver: DownloadProgressObserver
    private val mDlHandler = Handler(Looper.getMainLooper())
    private lateinit var mListAdapter: DownloadItemAdapter

    private var downloadItem = listOf<DownloadItem>()

    private val viewModel by viewModels<DownloadPageViewModel> { viewModelFactory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mDlMngr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        mDlObserver = DownloadProgressObserver(mDlHandler)
        mListAdapter = DownloadItemAdapter()

        viewModel.playerResList.observe(this, Observer { playerRes ->
            downloadItem = playerRes.map {
                DownloadItem(it.downloadId, it.videoId + " " + YouTubeStreamFormatCode.FORMAT_CODES[it.itag]?.let { format ->
                    format.resolution ?: format.bitrate ?: ""
                }, 0, 0)
            }
            mListAdapter.submitList(downloadItem)
            queryDownloadStatus()
        })
        viewModel.videoMeta.observe(this, Observer { meta ->

        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_download_manager, container, false)
            setupViews()
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setupWithNavController(findNavController())
    }

    private fun setupViews() {
        toolbar = rootView!!.findViewById(R.id.toolbar)
        rvDownloadStatus = rootView!!.findViewById(R.id.rv_download_status)
        rvDownloadStatus.apply {
            adapter = mListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().apply {
            contentResolver.registerContentObserver(CONTENT_URI, true, mDlObserver)
            viewModel.updateObservingDownloadRecord()
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().apply {
            contentResolver.unregisterContentObserver(mDlObserver)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            requireActivity().apply {
                contentResolver.unregisterContentObserver(mDlObserver)
            }
        } else {
            requireActivity().apply {
                contentResolver.registerContentObserver(CONTENT_URI, true, mDlObserver)
            }
        }
    }

    private fun queryDownloadStatus() {
        if (!viewModel.playerResList.value.isNullOrEmpty()) {
            FileDownloadHelper.getDownloadStatus(mDlMngr, viewModel.playerResList.value!!.map { it.downloadId }).
                let { mListAdapter.updateDownloadStatus(it) }
        }
    }

    inner class DownloadProgressObserver(handler: Handler): ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            Log.d(TAG, "progress updated")
            queryDownloadStatus()
        }
    }

    private inner class DownloadItemAdapter: ListAdapter<DownloadItem, DownloadItemAdapter.ViewHolder>(DIFF_CALLBACK) {

        override fun getItemId(position: Int): Long {
            return getItem(position).downloadId
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_download, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item)
        }

        fun updateDownloadStatus(status: Map<Long, DownloadStatus>) {
            for (i in 0 until itemCount) {
                val item = getItem(i)
                status[item.downloadId]?.apply {
                    item.currentSize = downloadedByte
                    item.totalSize = totalByte
                    notifyItemChanged(i)
                }
            }
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val id: TextView = itemView.findViewById(R.id.tv_id)
            val title: TextView = itemView.findViewById(R.id.tv_title)
            val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)

            fun bind(item: DownloadItem) {
                id.text = item.downloadId.toString()
                title.text = item.title
                progressBar.max = item.totalSize
                progressBar.progress = item.currentSize
            }
        }
    }

    private data class DownloadItem(
        var downloadId: Long,
        var title: String,
        var totalSize: Int,
        var currentSize: Int
    )

    companion object {
        const val TAG = "DownloadManagerFragment"
        private val CONTENT_URI = Uri.parse("content://downloads/my_downloads")
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DownloadItem>() {
            override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
                return oldItem.downloadId == newItem.downloadId
                        && oldItem.title == newItem.title
                        && oldItem.totalSize == newItem.totalSize
                        && oldItem.currentSize == newItem.currentSize
            }

            override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}
