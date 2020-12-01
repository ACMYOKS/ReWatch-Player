package com.amoscyk.android.rewatchplayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.ContentLoadingProgressBar
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment

class WebViewFragment : ReWatchPlayerFragment() {
    private var mToolbar: Toolbar? = null
    private var mWebView: WebView? = null
    private var mProgressBar: ContentLoadingProgressBar? = null
    private val arg by navArgs<WebViewFragmentArgs>()
    private var mShouldLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar!!.setupWithNavController(findNavController())
        mProgressBar = view.findViewById(R.id.progress_bar)
        mProgressBar!!.hide()
        mWebView = view.findViewById(R.id.web_view)
        mWebView!!.apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    if (view != null && request != null) {
                        view.loadUrl(request.url.toString())
                        mProgressBar?.show()
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    mProgressBar?.hide()
                    mShouldLoad = false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    mProgressBar?.hide()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (mShouldLoad) {
            mWebView?.apply {
                loadUrl(arg.url)
                mProgressBar?.show()
            }
        }
    }
}