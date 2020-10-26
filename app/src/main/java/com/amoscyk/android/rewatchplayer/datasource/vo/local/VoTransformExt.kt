package com.amoscyk.android.rewatchplayer.datasource.vo.local

import com.amoscyk.android.rewatchplayer.datasource.vo.*
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.YtInfo
import com.google.api.services.youtube.YouTube
import org.threeten.bp.Duration

fun YtInfo.getVideoMeta() = VideoMeta(
    videoId = videoDetails.videoId,
    title = videoDetails.title,
    channelId = videoDetails.channelId,
    channelTitle = videoDetails.author,
    description = videoDetails.shortDescription,
    duration = Duration.ofSeconds(
        videoDetails.lengthSeconds.toLongOrNull() ?: 0
    ).toString(),
    thumbnails = RPThumbnailDetails(),
    tags = videoDetails.keywords,
    itags = formats.map { it.itag } + adaptiveFormats.map { it.itag }
)

fun YouTube.Channels.List.getResponse(): RPChannelListResponse = execute().let { res ->
    RPChannelListResponse(
        request = this,
        items = res.items.map { it.toRPChannel() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}

fun YouTube.PlaylistItems.List.getResponse(): RPPlaylistItemListResponse = execute().let { res ->
    RPPlaylistItemListResponse(
        request = this,
        items = res.items.map { it.toRPPlaylistItem() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}

fun YouTube.Playlists.List.getResponse(): RPPlaylistListResponse = execute().let { res ->
    RPPlaylistListResponse(
        request = this,
        items = res.items.map { it.toRPPlayList() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}

fun YouTube.Search.List.getResponse(): RPSearchListResponse = execute().let { res ->
    RPSearchListResponse(
        request = this,
        items = res.items.map { it.toRPSearchResult() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}

fun YouTube.Subscriptions.List.getResponse() = execute().let { res ->
    RPSubscriptionListResponse(
        request = this,
        items = res.items.map { it.toRPSubscription() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}

fun YouTube.Videos.List.getResponse(): RPVideoListResponse = execute().let { res ->
    RPVideoListResponse(
        request = this,
        items = res.items.map { it.toRPVideo() },
        pageToken = this.pageToken,
        prevPageToken = res.prevPageToken,
        nextPageToken = res.nextPageToken,
        totalResults = res.pageInfo.totalResults ?: 0,
        resultsPerPage = res.pageInfo.resultsPerPage ?: 0
    )
}
