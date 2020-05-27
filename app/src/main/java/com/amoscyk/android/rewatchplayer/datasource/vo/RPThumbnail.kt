package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
import com.google.api.services.youtube.model.Thumbnail
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RPThumbnail(
    val url: String,
    val width: Long = -1,
    val height: Long = -1
): Parcelable

fun Thumbnail?.toRPThumbnail(): RPThumbnail? {
    if (this == null) return null
    return RPThumbnail(
        url.orEmpty(),
        width ?: -1,
        height ?: -1
    )
}