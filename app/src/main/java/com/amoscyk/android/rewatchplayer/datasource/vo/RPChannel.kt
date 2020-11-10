package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.Channel

data class RPChannel(
    val id: String,
    val title: String,
    val description: String,
    val keywords: String,
    val publishedAt: DateTime,
    val thumbnails: RPThumbnailDetails,
    val uploads: String,
    val viewCount: Long,
    val subscriberCount: Long,
    val videoCount: Long,
    val bannerMobileImageUrl: String,
    val bannerTabletImageUrl: String
)

fun Channel.toRPChannel() = RPChannel(
    id = id,
    title = snippet.title,
    description = snippet.description.orEmpty(),
    keywords = brandingSettings?.channel?.keywords.orEmpty(),
    publishedAt = snippet.publishedAt,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails(),
    uploads = contentDetails.relatedPlaylists?.uploads.orEmpty(),
    viewCount = statistics.viewCount?.toLong() ?: 0,
    subscriberCount = statistics.subscriberCount?.toLong() ?: 0,
    videoCount = statistics.videoCount?.toLong() ?: 0,
    bannerMobileImageUrl = brandingSettings?.image?.bannerMobileImageUrl.orEmpty(),
    bannerTabletImageUrl = brandingSettings?.image?.bannerTabletImageUrl.orEmpty()
)