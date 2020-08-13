package com.amoscyk.android.rewatchplayer.ui.downloads


import android.content.Context
import android.os.*
import android.view.*
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.DownloadStatus
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.MainViewModel
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadPageViewModel.MenuState
import com.amoscyk.android.rewatchplayer.util.DateTimeHelper
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper
import com.amoscyk.android.rewatchplayer.util.formatReadableByteUnit
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.download_manager_fragment_list_item.view.*
import kotlinx.android.synthetic.main.fragment_download_manager.view.*
import kotlinx.coroutines.launch

class DownloadManagerFragment : ReWatchPlayerFragment() {

    private val toolbar get() = view!!.toolbar
    private val rvDownloadStatus get() = view!!.rv_download_status
    private val mEmptyView get() = view!!.empty_view
    private var mListAdapter: DownloadItemAdapter? = null
    private val mDialogDelete by lazy {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure to remove the selected items?")
            .setPositiveButton(getString(R.string.confirm_text)) { _, _ ->
                lifecycleScope.launch {
                    val result = viewModel.deleteSelectedPlayerResource(requireContext())
                    if (result.status == Status.SUCCESS) {
                        Snackbar.make(view!!, "Item(s) deleted!", Snackbar.LENGTH_SHORT).show()
                        viewModel.getVideoMetaContainsPlayerResource()
                    } else {
                        Snackbar.make(view!!, (result.message as? String) ?: "error",
                            Snackbar.LENGTH_SHORT).show()
                    }
                    actionMode?.finish()
                }
            }
            .setNegativeButton(getString(R.string.cancel_text)) { _, _ -> }
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
    private val viewModel by viewModels<DownloadManagerViewModel> { viewModelFactory }

    private var downloadStatusMap = mapOf<Long, DownloadStatus>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListAdapter = DownloadItemAdapter().apply {
            setHasStableIds(true)
        }

        viewModel.videoMetas.observe(this, Observer { meta ->
            viewModel.updateDownloadStatus(requireContext())
            mListAdapter?.submitList(meta)
            mEmptyView.visibility = if (meta.isEmpty()) View.VISIBLE else View.GONE
        })
        viewModel.isEditMode.observe(this, Observer {
            mListAdapter?.setEditMode(it)
            if (it) {
                actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(
                    actionModeCallback
                )
            } else {
                actionMode?.finish()
            }
        })
        viewModel.selectedItems.observe(this, Observer {
            val downloadIds = it.flatMap { it.playerResources.map { it.downloadId } }
            val totalSize = downloadStatusMap.values.fold(0) { acc, status ->
                if (status.downloadId in downloadIds) acc + status.totalByte
                else acc
            }
            actionMode?.title = "${it.size} (${totalSize.toLong().formatReadableByteUnit()})"
        })
        viewModel.downloadStatus.observe(this, Observer {
            downloadStatusMap = it
            mListAdapter?.notifyDataSetChanged()
        })
        mainViewModel.archiveResult.observe(this, Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    viewModel.getVideoMetaContainsPlayerResource()
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
        return inflater.inflate(R.layout.fragment_download_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        toolbar.setupWithNavController(findNavController())
    }

    override fun onResume() {
        super.onResume()
        viewModel.getVideoMetaContainsPlayerResource()
    }

    private fun setupViews() {
        rvDownloadStatus.apply {
            adapter = mListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
    }


    private inner class DownloadItemAdapter: ListAdapter<VideoMetaWithPlayerResource, DownloadItemAdapter.ViewHolder>(DIFF_CALLBACK) {
        private val checkStatus = hashMapOf<String, Boolean>()
        private var isEditMode = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.download_manager_fragment_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            if (checkStatus[item.videoMeta.videoId] == null) checkStatus[item.videoMeta.videoId] = false
            holder.apply {
                ivPreview.load(YouTubeVideoThumbnailHelper.getStandardUrl(item.videoMeta.videoId))
                tvDuration.text = DateTimeHelper.getDisplayString(item.videoMeta.duration)
                tvTitle.text = item.videoMeta.title
                tvAuthor.text = item.videoMeta.channelTitle
                tvQuality.text = item.playerResources.joinToString { res ->
                    YouTubeStreamFormatCode.FORMAT_CODES[res.itag]?.let {
                        it.resolution ?: it.bitrate
                    }.orEmpty()
                }
                checkBox.isChecked = checkStatus[item.videoMeta.videoId]!!
                checkBox.visibility = if (isEditMode) View.VISIBLE else View.GONE
                btnPlay.visibility = if (isEditMode) View.GONE else View.VISIBLE
                val dlIds = item.playerResources.map { it.downloadId }
                var sum = 0L
                downloadStatusMap.forEach {
                    if (dlIds.contains(it.key)) {
                        sum += it.value.totalByte
                    }
                }
                tvFileSize.text = sum.formatReadableByteUnit()
            }
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).videoMeta.videoId.hashCode().toLong()
        }

        fun setEditMode(isOn: Boolean) {
            isEditMode = isOn
            if (!isOn) {
                checkStatus.keys.forEach { checkStatus[it] = false }
            }
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            val ivPreview: ImageView = itemView.iv_preview
            val tvDuration: TextView = itemView.tv_duration
            val tvTitle: TextView = itemView.tv_title
            val tvAuthor: TextView = itemView.tv_author
            val tvQuality: TextView = itemView.tv_quality
            val tvFileSize: TextView = itemView.tv_total_size
            val checkBox: CheckBox = itemView.checkbox_select
            val btnPlay: ImageButton = itemView.btn_play

            init {
                itemView.setOnClickListener {
                    if (viewModel.isEditMode.value == true) {
                        checkBox.isChecked = !checkBox.isChecked
                    } else {
                        findNavController().navigate(
                            DownloadManagerFragmentDirections.showDownloadedFile(
                                getItem(adapterPosition).videoMeta.videoId
                            )
                        )
                    }
                }
                itemView.setOnLongClickListener {
                    if (viewModel.isEditMode.value == true) {
                        checkBox.isChecked = !checkBox.isChecked
                    } else {
                        checkBox.isChecked = true
                    }
                    true
                }
                btnPlay.setOnClickListener {
                    mainActivity?.playVideoForId(getItem(adapterPosition).videoMeta.videoId, forceFindFile = true)
                }
                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    val item = getItem(adapterPosition)
                    checkStatus[item.videoMeta.videoId] = isChecked
                    val id = item.videoMeta.videoId
                    if (isChecked) viewModel.selectVideoMeta(id)
                    else viewModel.deselectVideoMeta(id)
                }
            }
        }
    }

    companion object {
        const val TAG = "DownloadManagerFragment"
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VideoMetaWithPlayerResource>() {
            override fun areItemsTheSame(oldItem: VideoMetaWithPlayerResource, newItem: VideoMetaWithPlayerResource): Boolean {
                return oldItem.videoMeta.videoId == newItem.videoMeta.videoId &&
                        oldItem.videoMeta.title == newItem.videoMeta.title &&
                        oldItem.videoMeta.channelTitle == newItem.videoMeta.channelTitle
            }

            override fun areContentsTheSame(oldItem: VideoMetaWithPlayerResource, newItem: VideoMetaWithPlayerResource): Boolean {
                return oldItem == newItem
            }
        }
    }

}
