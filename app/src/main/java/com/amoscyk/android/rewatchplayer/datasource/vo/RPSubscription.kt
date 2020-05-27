package com.amoscyk.android.rewatchplayer.datasource.vo

import com.google.api.client.util.DateTime
import com.google.api.services.youtube.model.Subscription

data class RPSubscription(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String,
    val publishedAt: DateTime,
    val thumbnails: RPThumbnailDetails
) {
    /* for preview use only */
    fun toRPChannel() = RPChannel(
        id = channelId,
        title = title,
        description = description,
        keywords = "",
        publishedAt = publishedAt,
        thumbnails = thumbnails,
        uploads = "",
        viewCount = 0,
        subscriberCount = 0,
        videoCount = 0,
        bannerMobileImageUrl = "",
        bannerTabletImageUrl = ""
    )
}

fun Subscription.toRPSubscription() = RPSubscription(
    id = id,
    channelId = snippet.resourceId.channelId,
    title = snippet.title,
    description = snippet.description,
    publishedAt = snippet.publishedAt,
    thumbnails = snippet.thumbnails.toRPThumbnailDetails()
)