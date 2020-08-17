package com.amoscyk.android.rewatchplayer.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.google.android.exoplayer2.util.Log
import java.util.*

class ContentListFragment : ReWatchPlayerFragment() {

    private lateinit var loadingView: ContentLoadingProgressBar
    private lateinit var recyclerView: RecyclerView

    private var rvSetup : ((RecyclerView) -> Unit)? = null
    private var shouldShowLoading = false
    private var hasPendingViewChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_content_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rv_content)
        loadingView = view.findViewById(R.id.loading_view)
        loadingView.hide()
        rvSetup?.invoke(recyclerView)
        if (hasPendingViewChange) {
            handleViewChange()
        }
    }

    // save setup procedures which should be proceeded when recyclerView is created
    fun setupRecyclerView(callback: (recyclerView: RecyclerView) -> Unit) {
        rvSetup = callback
    }

    fun beginLoadingView() {
        setShouldShowLoading(true)
    }

    fun endLoadingView() {
        setShouldShowLoading(false)
    }

    private fun setShouldShowLoading(value: Boolean) {
        shouldShowLoading = value
        if (view == null) {
            hasPendingViewChange = true
        } else {
            hasPendingViewChange = false
            handleViewChange()
        }
    }

    private fun handleViewChange() {
        if (shouldShowLoading) {
            loadingView.show()
            recyclerView.visibility = View.INVISIBLE
        } else {
            loadingView.hide()
            recyclerView.visibility = View.VISIBLE
        }
        hasPendingViewChange = false
    }



}