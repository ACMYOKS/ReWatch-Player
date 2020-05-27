package com.amoscyk.android.rewatchplayer.ui.downloads


import android.content.Context
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.*
import coil.api.load
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.datasource.vo.Status
import com.amoscyk.android.rewatchplayer.datasource.vo.local.VideoMetaWithPlayerResource
import com.amoscyk.android.rewatchplayer.ui.downloads.DownloadPageViewModel.MenuState
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.amoscyk.android.rewatchplayer.util.YouTubeVideoThumbnailHelper
import com.amoscyk.android.rewatchplayer.viewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.download_manager_fragment_list_item.view.*
import kotlinx.coroutines.launch

class DownloadManagerFragment : ReWatchPlayerFragment() {

    private var rootView: View? = null
    private lateinit var toolbar: Toolbar
    private lateinit var rvDownloadStatus: RecyclerView
    private lateinit var mListAdapter: DownloadItemAdapter
    private val mDialogDelete by lazy {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure to remove the selected items?")
            .setPositiveButton("ok") { _, _ ->
                lifecycleScope.launch {
                    val result = viewModel.deleteSelectedPlayerResource(requireContext())
                    if (result.status == Status.SUCCESS) {
                        Snackbar.make(view!!, "Item(s) deleted!", Snackbar.LENGTH_SHORT).show()
                        viewModel.getVideoMetaContainsPlayerResource()
                    } else {
                        Snackbar.make(view!!, (result.message as? String) ?: "error",
                            Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("cancel") { _, _ -> }
            .create()
    }

    private val viewModel by viewModels<DownloadManagerViewModel> { viewModelFactory }

    private val mOnBackPressedCallback = object : OnBackPressedCallback(false) {
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
        mListAdapter = DownloadItemAdapter()

        viewModel.videoMeta.observe(this, Observer { meta ->
            mListAdapter.submitList(meta)
        })
        viewModel.isEditMode.observe(this, Observer {
            mListAdapter.setEditMode(it)
            mOnBackPressedCallback.isEnabled = it
        })
        viewModel.menuState.observe(this, Observer {
            it?.let { setMenuItemForState(it) }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

    override fun onResume() {
        super.onResume()
        requireActivity().apply {
            viewModel.getVideoMetaContainsPlayerResource()
        }
    }

    private fun setupViews() {
        toolbar = rootView!!.findViewById<Toolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.downloaded_video_option_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        mDialogDelete.show()
                    }
                    R.id.details -> {
                        viewModel.getSelectedVideoMetas().firstOrNull()?.let {
                            findNavController().navigate(
                                DownloadManagerFragmentDirections.showDownloadedFile(it.videoMeta.videoId)
                            )
                        }
                    }
                }
                true
            }
        }
        rvDownloadStatus = rootView!!.findViewById<RecyclerView>(R.id.rv_download_status).apply {
            adapter = mListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        }
    }

    private fun setMenuItemForState(state: MenuState) {
        with (toolbar.menu) {
            val delete = findItem(R.id.delete)
            val details = findItem(R.id.details)
            when (state) {
                MenuState.NORMAL -> {
                    delete.isVisible = false
                    details.isVisible = false
                }
                MenuState.SELECT_SINGLE -> {
                    delete.isVisible = true
                    details.isVisible = true
                }
                MenuState.SELECT_MULTI -> {
                    delete.isVisible = true
                    details.isVisible = false
                }
            }
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
                ivPreview.load(YouTubeVideoThumbnailHelper.getDefaultUrl(item.videoMeta.videoId))
                tvTitle.text = item.videoMeta.title
                tvAuthor.text = item.videoMeta.channelTitle
                tvQuality.text = item.playerResources.joinToString { res ->
                    YouTubeStreamFormatCode.FORMAT_CODES[res.itag]?.let {
                        it.resolution ?: it.bitrate
                    }.orEmpty()
                }
                checkBox.isChecked = checkStatus[item.videoMeta.videoId]!!
                checkBox.visibility = if (isEditMode) View.VISIBLE else View.GONE
            }
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
            val tvTitle: TextView = itemView.tv_title
            val tvAuthor: TextView = itemView.tv_author
            val tvQuality: TextView = itemView.tv_quality
            val checkBox: CheckBox = itemView.checkbox_select

            init {
                itemView.setOnClickListener {
                    if (viewModel.isEditMode.value == true) {
                        checkBox.isChecked = !checkBox.isChecked
                    } else {
                        mainActivity?.playVideoForId(getItem(adapterPosition).videoMeta.videoId)
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
                        oldItem.videoMeta.channelTitle == newItem.videoMeta.channelTitle &&
                        oldItem.videoMeta.bookmarked == newItem.videoMeta.bookmarked
            }

            override fun areContentsTheSame(oldItem: VideoMetaWithPlayerResource, newItem: VideoMetaWithPlayerResource): Boolean {
                return oldItem == newItem
            }
        }
    }

}
